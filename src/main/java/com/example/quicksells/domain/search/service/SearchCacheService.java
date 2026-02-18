package com.example.quicksells.domain.search.service;

import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.item.repository.ItemRepository;
import com.example.quicksells.domain.search.model.response.KeywordCount;
import com.example.quicksells.domain.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchCacheService {

    private final ItemRepository itemRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SearchRankingSnapshotService searchRankingSnapshotService;
    private final SearchRepository searchRepository;

    //Realtime ranking(10min sliding)
    private static final String REALTIME_PREFIX = "relation:keyword:"; //검색 기록
    private static final String REALTIME_ACTIVE_KEY = "relation:active"; //활성 키워드 목록
    private static final int WINDOW_MINUTES = 10; //10분 윈도우
    private static final int TTL_MINUTES = 15;//TTL 15분 윈도우

    // Weekly ranking (rolling 7 days)
    private static final String WEEK_RANKING_PREFIX = "rank:weekly:"; //주일 랭킹 키 prefix
    private static final Duration WEEKLY_RANKING_TTL = Duration.ofDays(7); //일일 랭킹 키 prefix

    /**
     * 캐시에 적용된 검색 메소드
     *
     * @param keyword  검색어
     * @param pageable 패이징 정보
     * @return 입력한 검색어의 상품 목록
     */
    //DB에서 상품 검색
    @Cacheable(value = "searchResult") //CacheConfig에서 캐시이름
    @Transactional(readOnly = true) //검색어, 페이지 번호, 페이지 사이즈
    public Page<Item> cachedSearch(String keyword, Pageable pageable) {

        //실제 DB 검색 수행
        return itemRepository.searchItems(keyword, pageable);
    }

    /**
     * 주간 인기 검색어 TOP 조회
     * <p>
     * POPULAR_RANKING_KEY (ZSET)
     * score = 누적 검색 횟수
     * reverseRange score 높은 순으로 TOP10 키워드만 조회
     *
     * @return TOP10 키워드 리스트 없으면 빈리스트 출력
     */
    public List<String> getPopularKeywordsList() {

        String weeklyKey = WEEKLY_RANKING_KEY();

        //score 내림차순으로 카운트 상위 10개 조회
        Set<Object> count = redisTemplate.opsForZSet().reverseRange(weeklyKey, 0, 9);


        //  Null 방어 -> Redis 비어 있거나 조회 실패시 빈 리스트
        if (count == null || count.isEmpty()) {
            return getFallbackKeywords(); //폴백 기능 추가
        }

        //Object를 String으로 반환
        return count.stream()
                .map(keyword -> String.valueOf(keyword.toString()))
                .toList();
    }

    /**
     * Redis에 있는 캐시 정보를 10분마다 DB로 복사 및 백업
     * <p>
     * Redis는 메모리 기반이라 영속성이 약함
     * -> 주기적으러 TOP10을 DB에 저장해 백업 가능하게 만듬
     * POPULAR_RANKING_KEY에서 (keyword, score) TOP10 조회
     * DB에 upsert(있으면 update, 없으면 insert)
     */

    @Scheduled(fixedDelay = 1_800_000) //30분 마다
    @Transactional
    public void snapshotTop10toDb() {

        // 주일 랭킹 키
        String weeklyKey = WEEKLY_RANKING_KEY();

        //조회수 기준 내림차순, 상위 10개 데이터 조회
        Set<ZSetOperations.TypedTuple<Object>> dailyTop10 =
                redisTemplate.opsForZSet().reverseRangeWithScores(weeklyKey, 0, 9);

        if (dailyTop10 == null || dailyTop10.isEmpty()) return;

        //Top10을 DB에 저장, 갱신
        for (ZSetOperations.TypedTuple<Object> data : dailyTop10) {
            if (data == null || data.getValue() == null || data.getScore() == null) continue;

            String keyword = data.getValue().toString();
            Long dailyCount = data.getScore().longValue();

            //DB 저장 일일 카운트
            searchRankingSnapshotService.upsertSnapshot(keyword, dailyCount);
        }
    }

    /**
     * DB 데이터 삭제 (최신 내역 7일 전 기록 삭제)
     */
// 매일 자정에 실행
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanDBSearchLogs() {

        //시간 지장 (7일 전 내역 삭제)
        LocalDateTime oneWeek = LocalDateTime.now().minusDays(7);

        //데이터 삭제
        searchRepository.deletedOldLogs(oneWeek);
    }

    /**
     * 주간 키 생성용
     */
    private String WEEKLY_RANKING_KEY() {
        return "rank:weekly";
    }

    /**
     *
     * @param keyword
     */
    private void incrementRanking(String keyword) {

        // 주간 랭킹 키
        String weeklyKey = WEEKLY_RANKING_KEY();

        //주간 랭킹 조회수 +1
        redisTemplate.opsForZSet().incrementScore(weeklyKey, keyword, 1);

        //TTL 설정
        redisTemplate.expire(weeklyKey, WEEKLY_RANKING_TTL);
    }

    /**
     * 실시간 인기 검색어 TOP10(슬라이딩 윈도우 기반 10분)
     * 1. 최근에 활성화된 키워드 목록 조회
     * 2. 각 키워드별로 윈도우 범위 내 검색 수 계산
     * 3. 의미 없는 키워드 정리 -> 슬라이딩 위도우 시간 동안 키워드 카운트 0되면 삭제
     * 4. 검색 수 기준 내림차순 정렬 -> Top10반환
     * 5. 순위가 빈 경우 빈리스트 반환 -> 실시간 트래픽만 반영하게함
     *
     */
    public List<String> getRealtimeTop10() {
        long now = System.currentTimeMillis();
        long windowStart = now - (WINDOW_MINUTES * 60 * 1000L); //10분
        long ttlStart = now - TTL_MINUTES * 60 * 1000L; // 캐시 생존 시간 15분

        //REALTIME_ACTIVE_KEY -> 최근 검색된 키워드 모아둔 인덱스 역할
        Set<ZSetOperations.TypedTuple<Object>> activeKeywords = redisTemplate.opsForZSet().rangeByScoreWithScores(REALTIME_ACTIVE_KEY, ttlStart, now);

        // 활성 키워드 자체가 없다면 fallback
        if (activeKeywords == null || activeKeywords.isEmpty()) {
            //빈리스트 반환
            return List.of();
        }

        List<KeywordCount> keywordCounts = new ArrayList<>();

        // 각 키워드별로 실시간 검색 수 계산
        for (ZSetOperations.TypedTuple<Object> tuple : activeKeywords) {
            String keyword = String.valueOf(tuple.getValue());
            Long lastSearchTime = tuple.getScore().longValue();
            String keywordKey = REALTIME_PREFIX + keyword;

            //오래된 데이터 삭제 (메모리 절약)
            redisTemplate.opsForZSet().removeRangeByScore(keywordKey, 0, windowStart);


            //현재 윈도우 기준 검색 횟수 계싼
            Long count = redisTemplate.opsForZSet().count(keywordKey, windowStart, now);

            //윈도우 내 검색 횟수 계산
            if (count != null && count > 0) {
                keywordCounts.add(new KeywordCount(keyword, count, lastSearchTime));
            } else {

                //  검색 횟수가 0인 경우 키워드 삭제
                redisTemplate.delete(keywordKey);

                //의미 없으면 active에서 제거
                redisTemplate.opsForZSet().remove(REALTIME_ACTIVE_KEY, keyword);
            }
        }

        //실시간 검색어 정렬해서 내림차순 정렬 후 TOP10 반환
        List<String> realtimeTop10 = keywordCounts.stream()
                .sorted((a, b) -> {
                    int countCompare = Long.compare(b.getCount(), a.getCount());
                    if (countCompare != 0) {
                        return countCompare; //count가 다르면 count 정렬
                    }
                    //count가 같으면 최근 검색 시간으로 정렬(내림차순)
                    //정렬 1순위 count 내림차순, 2순위 lastSearchTime(내림차순 = 최신순)
                    return Long.compare(b.getLastSearchTime(), a.getLastSearchTime());
                })
                .limit(10)
                .map(keywordCount -> keywordCount.getKeyword())
                .toList();

        if (realtimeTop10.isEmpty()) return getFallbackKeywords();
        return realtimeTop10;
    }

    /**
     * 실시간 검색어 기록 (10분 슬라이딩 윈도우)
     *
     * @param keyword
     */
    private void addRealTimeRecord(String keyword) {

        //키 예시
        String key = REALTIME_PREFIX + keyword;

        //현재 시각 (슬라이딩 윈도우 기준점)
        long now = System.currentTimeMillis();

        //ZSET에 UUID와 현재 시간 저장 -> 최근 10분 동안 몇 번 검색됐는지 score,count로 확인
        redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);

        // 현재 활성화된 검색어 목록 관리용 ZSET -> (오래된 키 자동 제거)
        redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);//15분

        //검색된 키워드들만 빠르게 조회하기 위함
        redisTemplate.opsForZSet().add(REALTIME_ACTIVE_KEY, keyword, now);

        //활성 키워드 인덱스 TTL 설정
        redisTemplate.expire(REALTIME_ACTIVE_KEY, TTL_MINUTES, TimeUnit.MINUTES);//15분
    }

    /**
     * 검색어 중복 클릭 방지 기능 (첫 클릭에만 카운트)
     *
     * @param clientKey 사용자 구분하는 값
     * @param keyword   사용자가 입력한 검색어
     *                  주간 랭킹 카운트
     *                  실시간 검색어 기록
     */
    public void notDoubleClick(String clientKey, String keyword) {
        // 키워드 검증
        if (keyword == null) return;

        //공백 정규화
        String writeKeyword = keyword.trim().replaceAll("\\s+", " ");
        if (writeKeyword.isEmpty()) {
            return;
        }

        // 중복 방지 키 설정 -> popular:dup:{사용자 식별자}:{검색어}
        String dupKey = "popular:dup:" + clientKey + ":" + writeKeyword;

        //Redis에 중복 확인용 키 저장 -> 첫 클릭 후 중복 클릭시 3초동안 카운트 X
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(dupKey, "1", Duration.ofSeconds(3));

        // 키가 없을 때 저장 후 3초 클릭 방지
        if (!Boolean.TRUE.equals(first)) { //setIfAbsent()는 처음 저장 시 true = 성공임
            //중복 클릭인 경우 카운트 X
            return;
        }

        //주간 랭킹 기록 + TTL
        incrementRanking(writeKeyword);

        //실시간 검색어 기록
        addRealTimeRecord(writeKeyword);
    }

    /**
     * 주간
     *
     * @return
     */
    private List<String> getWeekTop10() {

        //주간인기검색 키
        String weeklyKey = WEEKLY_RANKING_KEY();

        //주간 렝킹 Top10
        Set<Object> set = redisTemplate.opsForZSet().reverseRange(weeklyKey, 0, 9);

        //리턴만 하면 NPE 발생하여 수정
        if (set == null || set.isEmpty()) return List.of();
        return set.stream().map(Object::toString).toList();
    }

    /**
     * 우선 순위
     *
     * @return 검색어 리스트 반환
     */
    public List<String> getFallbackKeywords() {

        //Redis/Cache 기반 인기 검색어 -> 최근까지 검색된 기록 우선순위
        List<String> weeklyTop10 = getWeekTop10();
        if (!weeklyTop10.isEmpty()) {
            return weeklyTop10;
        }

        //DB에서 조회
        List<String> dbTop10 = searchRepository.findTop10ByOrderByTotalCountDesc(PageRequest.of(0, 10));
        return dbTop10;
    }
}

