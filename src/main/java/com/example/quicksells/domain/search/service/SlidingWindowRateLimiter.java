package com.example.quicksells.domain.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;


    /**
     * 슬라이딩 윈도우 레이트 리미터(검색 요청 제한)
     * @param userId 유저 식별자
     * @param windowSeconds 윈도우 크기(초) 예: 60초
     * @param maxRequests 윈도우 안에서 허용되는 최대 요청 수
     * 동작 방식 -> 레디스 ZSET에 요청 마다 requestId, 요청시간ms를 저장한다.
     * 1. 너무 오래된 검색은 윈도우 밖으로 이동되며 기록 삭제
     * 2. 윈도우 안에 남아있는 요청 count
     * 3. 제한 초과면 차단(false)
     * 4. 통과(true)시 요청은 ZSET에 추가하는 방식
     *
     * @return
     */
    //슬라이딩 윈도우
    public boolean isAllowed(Long userId, int windowSeconds, int maxRequests) {

        //현재 시간 (ms)를 score로 저장할 값
        long now = System.currentTimeMillis();

        // 윈도우 시작 시간 = 지금 시간 - windowSeconds
        // windowSeconds가 60이면 60초 안에 들어온 요청만 인정
        long windowStart = now - windowSeconds * 1000L;

        // 유저마다 키를 각각 가짐
        String key = "rate_limit:search:user:" + userId;

        try {
            //1. 윈도우 밖의 오래된 요청 삭제
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

            //2. 현재 윈도우 내 요청 수 확인
            Long currenCount = redisTemplate.opsForZSet().count(key, windowStart, now);

            //제한 초과시 바로 차단 -> maxRequests 이상이면 false 리턴
            if (currenCount != null && currenCount >= maxRequests) {
                log.warn("검색 Rate Limit 초과: userId={}, count={}/{}",
                        userId, currenCount, maxRequests);
                return false;
            }

            //3. 현재 요청 추가
            String requestId = UUID.randomUUID().toString();
            redisTemplate.opsForZSet().add(key, requestId, now);

            //4. TTL 설정 -> 메모리 절약
            // 키는 최근 몇 초 정보만 필요
            // 유저가 요청 안하면 키 삭제해 메모리 절약
            // windowSeconds + 60 여유를 한 이유 -> 조금 늦게 지워도 안전하게 남겨 두기 위함
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 60));
            return true;

        } catch (Exception e) {
            log.error("Rate Limiter 오류 ", e);
            return true;
        }
    }

    //남은 요청 가능 횟수

    /**
     *
     * @param userId
     * @param windowSeconds
     * @param maxRequest
     * @return 0이상 점수 반환 -> 윈도우 내 요청 수를 세서 maxRequest - count
     */
    public int getRemainingRequests(Long userId, int windowSeconds, int maxRequest) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;

        String key = "rate_limit:search:user:" + userId;

        try {
            // count가 null일 수 있으니 0 처리
            Long count = redisTemplate.opsForZSet().count(key, windowStart, now);
            return Math.max(0, maxRequest - (count != null ? count.intValue() : 0));
        } catch (Exception e) {
            return maxRequest;
        }
    }

    //다음 요청 가능 시간

    /**
     *
     * @param userId 제한을 확인할 사용자 Id
     * @param windowSeconds 윈도우 크기 재한
     * @return 재시도까지 남은 시간
     */
    public long getAfter(Long userId, int windowSeconds) {

        // 사용자별 Rate Limit용 Redis 키
        String key = "rate_limit:search:user:" + userId;

        try {
            //가장 오래된 요청 1개 가져오기(score 작은 값)
            // - rang(key,0,0)는 정렬된 순서상 첫 번째 = 가장 오래된 요청
            Set<Object> oldeset = redisTemplate.opsForZSet().range(key, 0, 0);


            if (oldeset == null || oldeset.isEmpty()) {
                return 0; //요청 바로 가능
            }

            //가장 오래된 요청의 값
            Object oldSetRequest = oldeset.iterator().next();

            // 그 요소의 score ms 시간 조회
            Double oldestScore = redisTemplate.opsForZSet()
                    .score(key, oldSetRequest);

            //score 못 가져오면 안전하게 바로 가능 처리
            if (oldestScore == null) {
                return 0; // 요청 바로 가능
            }

            // 가장 오래된 요청 시각 (ms)
            long oldSetTime = oldestScore.longValue();

            //현재 시각(ms)
            long now = System.currentTimeMillis();

            //슬라이딩 윈도우가 끝나는 시간 = 가장 오래된 요청 시간 + 윈도우 크기
            long windowEnd = oldSetTime + (windowSeconds * 1000L);

            //윈도우가 끝나는 시점까지 남은 시간 -> windowEnd이 현재 보다 과거면 윈도우를 벗어남
            return Math.max(0, (windowEnd - now) / 1000);

        } catch (Exception e) {
            log.error("getRetryAfterSeconds 오류: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 특정 유저의 제한 가록을 초기화
     * ZSET 키를 삭제해서 count가 0되게 만듦
     * @param userId 제한을 초기화할 사용자 Id
     */
    public void resetLimit(Long userId) {

        //사용자별 Rate Limit키
        String key = "rate_limit:search:user:" + userId;

        //ZSET 삭제 -> 모든 요청 기록 삭제
        redisTemplate.delete(key);

        log.info("Rate Limit 초가화: userId={}", userId);
    }
}

