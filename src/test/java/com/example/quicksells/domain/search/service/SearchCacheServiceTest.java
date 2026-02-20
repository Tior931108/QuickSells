package com.example.quicksells.domain.search.service;

import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.item.repository.ItemRepository;
import com.example.quicksells.domain.search.repository.SearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.core.parameters.P;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchCacheServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SearchRankingSnapshotService searchRankingSnapshotService;

    @Mock
    private SearchRepository searchRepository;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SearchCacheService searchCacheService;

    // 공통 상수
    private static final String POPULAR_RANKING_KEY = "ranking:keyword";
    private static final String REALTIME_ACTIVE_KEY = "relation:active";
    private static final String REALTIME_PREFIX = "relation:keyword:";
    private static final String DAILY_RANKING_PREFIX = "rank:daily:";

    private void subZSet() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    private void subValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }



    @Test
    @DisplayName("cachedSearch - DB 검색 정상 호출")
    void cachedSearch_ok() {
        // Given
        String keyword = "아이폰";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> mockPage = mock(Page.class);

        when(itemRepository.searchItems(eq(keyword), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<Item> result = searchCacheService.cachedSearch(keyword, pageable);

        // Then
        assertEquals(mockPage, result);
        verify(itemRepository).searchItems(eq(keyword), eq(pageable));
    }
    //==========getPopularKeywordsList===========/

    @Test
    @DisplayName("인기 검색어 - Redis 비어있으면 빈 리스트")
    void getPopularKeywordsList() {

        //given
        subZSet();
        Set<Object> keywords = new LinkedHashSet<>();
        keywords.add("아이폰");
        keywords.add("맥북M4");
        keywords.add("아이패드");

        when(zSetOperations.reverseRange(eq(POPULAR_RANKING_KEY), eq(0L), eq(9L)))
                .thenReturn(null);

        //when
        List<String> result = searchCacheService.getPopularKeywordsList();

        //then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("인기 검색어 - 빈 Radis면 빈리스트")
    void getPopularKeywordsListNull() {
        //given
        subZSet();
        when(zSetOperations.reverseRange(eq(POPULAR_RANKING_KEY), eq(0L), eq(9L)))
                .thenReturn(null);

        //when
        List<String> result = searchCacheService.getPopularKeywordsList();

        //then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("인기 검색어 - 정상 반환")
    void getPopularKeywordsList_return() {
        // Given
        subZSet();
        Set<Object> keywords = new LinkedHashSet<>();
        keywords.add("아이폰");
        keywords.add("맥북");

        when(zSetOperations.reverseRange(eq(POPULAR_RANKING_KEY), eq(0L), eq(9L)))
                .thenReturn(keywords);

        // When
        List<String> result = searchCacheService.getPopularKeywordsList();

        // Then
        assertEquals(2, result.size());
        assertEquals("아이폰", result.get(0));
    }

//=========snapshot=========//

    @Test
    @DisplayName("인기 검색어 - 빈 Set이면 빈리스트")
    void snapshotTop10toDb() {
        //given
        subZSet();
        when(zSetOperations.reverseRange(eq(POPULAR_RANKING_KEY), eq(0L), eq(9L)))
                .thenReturn(Collections.emptySet());
        //when
        List<String> result = searchCacheService.getPopularKeywordsList();

        //then
        assertTrue(result.isEmpty());
    }
    @Test
    @DisplayName("snapshotTop10toDb - tuple null이면 건너뜀")
    void snapshotTop10toDb_tuple_null() {
        // Given
        subZSet();
        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        ZSetOperations.TypedTuple<Object> nullTuple = mock(ZSetOperations.TypedTuple.class);
        when(nullTuple.getValue()).thenReturn(null);  // null


        Set<ZSetOperations.TypedTuple<Object>> tuples = new LinkedHashSet<>();
        tuples.add(nullTuple);

        when(zSetOperations.reverseRangeWithScores(eq(dailyKey), eq(0L), eq(9L)))
                .thenReturn(tuples);

        // When
        searchCacheService.snapshotTop10toDb();

        // Then (null이라 저장 안 됨)
        verifyNoInteractions(searchRankingSnapshotService);
    }

    //=============notDoubleClick============/
    @Test
    @DisplayName("낫더블체크 null인 경우 리턴")
    void notDoubleClick_keyword_mull() {
        //given

        //when
        searchCacheService.notDoubleClick("user-1", null);

        //then
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("낫더블체크 공백일 경우 리턴")
    void notDoubleClick_keyword_isE() {
        //given

        //when
        searchCacheService.notDoubleClick("user-1", " ");

        //then
    }

    @Test
    @DisplayName("첫 클릭 시 카운트 증가")
    void notDoubleClick_first() {
        //given
        subZSet();
        subValue();
        String clientKey = "user-1";
        String keyword = "아이폰";
        String dupKey = "popular:dup:" + clientKey + ":" + keyword;
        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        when(valueOperations.setIfAbsent(eq(dupKey), eq("1"), eq(Duration.ofSeconds(3))))
                .thenReturn(true);

        //when

        searchCacheService.notDoubleClick(clientKey, keyword);

        //then
        //일일 랭킹
        verify(zSetOperations).incrementScore(eq(dailyKey), eq(keyword), eq(1.0));
        //실시간 기록 추가 확인
        verify(zSetOperations).add(eq(REALTIME_PREFIX + keyword), anyString(), anyDouble());
    }

    @Test
    @DisplayName("notDoubleClick - setIfAbsent null이면 카운트 안 함")
    void notDoubleClick_setIfAbsent_null() {
        // Given
        subValue();
        String clientKey = "user-1";
        String keyword = "아이폰";
        String dupKey = "popular:dup:" + clientKey + ":" + keyword;

        when(valueOperations.setIfAbsent(eq(dupKey), eq("1"), eq(Duration.ofSeconds(3))))
                .thenReturn(null);

        // When
        searchCacheService.notDoubleClick(clientKey, keyword);

        // Then (카운트 증가 안 됨)
        verify(zSetOperations, never()).incrementScore(anyString(), any(), anyDouble());
    }

    @Test
    @DisplayName("더블 클릭 시 중복 방지")
    void notDoubleClick_second() {
        //given
        subValue();
        String clientKey = "user-1";
        String keyword = "아이폰";
        String dupKey = "popular:dup:" + clientKey + ":" + keyword;

        when(valueOperations.setIfAbsent(eq(dupKey), eq("1"), eq(Duration.ofSeconds(3))))
                .thenReturn(false);

        //when
        searchCacheService.notDoubleClick(clientKey, keyword);

        //then
        // 숫자 증가 방지
        verify(zSetOperations, never()).incrementScore(anyString(), any(), anyDouble());
    }

    @Test
    @DisplayName("notDoubleClick - 앞뒤 공백 제거 후 관리")
    void notDoubleClick_not_() {
        //given
        subZSet();
        subValue();
        String clientKey = "user-1";
        String keyword = " 아이폰 "; //공백
        String trimmedKeyword = "아이폰";
        String dupKey = "popular:dup:" + clientKey + ":" + trimmedKeyword;

        when(valueOperations.setIfAbsent(eq(dupKey), eq("1"), eq(Duration.ofSeconds(3))))
                .thenReturn(true);

        //when
        searchCacheService.notDoubleClick(clientKey, keyword);

        //then
        verify(zSetOperations).add(
                eq(REALTIME_PREFIX + trimmedKeyword), anyString(), anyDouble()
        );
    }

    //===========getRealtimeTop10=======/
    @Test
    @DisplayName("getRealtimeTop10 - 활성 키워드 null이면 fallback")
    void getRealtimeTop10_activityKeyword_null() {
        //given
        subZSet();

        //일일 TOP10 fallback
        Set<Object> dailySet = new LinkedHashSet<>();
        dailySet.add("아이폰");
        String dailyKey = DAILY_RANKING_PREFIX + LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        when(zSetOperations.rangeByScore(eq(REALTIME_ACTIVE_KEY), anyDouble(), anyDouble()))
                .thenReturn(null);
        when(zSetOperations.reverseRange(eq(dailyKey), eq(0L), eq(9L)))
                .thenReturn(dailySet);

        //when
        List<String> result = searchCacheService.getRealtimeTop10();

        //then
        assertFalse(result.isEmpty());
    }


    @Test
    @DisplayName("getRealtimeTop10 - 키워드 5개 미만 fallback")
    void getFallbackKeywords_keyword_Lower5() {
        //given
        subZSet();

        // 활성 키워드 3개만 설정
        Set<Object> activeKeywords = new LinkedHashSet<>();
        activeKeywords.add("아이폰");
        activeKeywords.add("맥북");
        activeKeywords.add("에어팟");

        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        when(zSetOperations.rangeByScore(eq(REALTIME_ACTIVE_KEY), anyDouble(), anyDouble()))
                .thenReturn(activeKeywords);

        when(zSetOperations.count(anyString(), anyDouble(), anyDouble()))
                .thenReturn(5L);

        //fallback용 일일 TOP10
        Set<Object> dailySet = new LinkedHashSet<>();
        dailySet.add("갤럭시플립폰");
        when(zSetOperations.reverseRange(eq(dailyKey), eq(0L), eq(9L)))
                .thenReturn(dailySet);

        //when
        List<String> result = searchCacheService.getRealtimeTop10();


        //then
        assertFalse(result.isEmpty());
        assertTrue(result.size() <= 10);
    }


    @Test
    @DisplayName("snapshotTop10toDb - score null이면 건너뜀")
    void snapshotTop10toDb_score_null() {
        subZSet();
        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        ZSetOperations.TypedTuple<Object> nullScoreTuple = mock(ZSetOperations.TypedTuple.class);
        when(nullScoreTuple.getValue()).thenReturn("아이폰");
        when(nullScoreTuple.getScore()).thenReturn(null);  // score가 null

        Set<ZSetOperations.TypedTuple<Object>> tuples = new LinkedHashSet<>();
        tuples.add(nullScoreTuple);

        when(zSetOperations.reverseRangeWithScores(eq(dailyKey), eq(0L), eq(9L)))
                .thenReturn(tuples);
        //when
        searchCacheService.snapshotTop10toDb();

        //then
        verifyNoInteractions(searchRankingSnapshotService);

    }
    //========getFallbackKeywords========

    @Test
    @DisplayName("getFallbackKeywords - 일일 TOP10 있으면 반환")
    void getFallbackKeywords_dailyTop10() {
        //given
        subZSet();

        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        Set<Object> dailySet = new LinkedHashSet<>();
        dailySet.add("아이폰");
        dailySet.add("맥북");

        when(zSetOperations.reverseRange(eq(dailyKey), eq(0L), eq(9L)))
                .thenReturn(dailySet);

        //when
        List<String> result = searchCacheService.getFallbackKeywords();

        //then
        assertEquals(2, result.size());

        //DB 조회 안함
        verifyNoInteractions(searchRepository);
    }

    @Test
    @DisplayName("getFallbackKeywords - 일일 TOP10 없으면 DB 조회")
    void getFallbackKeywords_DBGet() {
        //given
        subZSet();
        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        //일일 DB에서 비어있음
        when(zSetOperations.reverseRange(eq(dailyKey), eq(0L), eq(9L)))
                .thenReturn(Collections.emptySet());

        List<String> dbKeywords = List.of("아이폰", "맥북");

        when(searchRepository.findTop10ByOrderByTotalCountDesc(any()))
                .thenReturn(dbKeywords);
        //when
        List<String> result = searchCacheService.getFallbackKeywords();

        //then
        assertEquals(2, result.size());
        verify(searchRepository).findTop10ByOrderByTotalCountDesc(any());
    }

    @Test
    @DisplayName("sanpshotTop10toDb 저장 완료")
    void snapshotTop10toDbSave() {
        //given
        subZSet();
        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        // TypedTuple Mock 설정
        ZSetOperations.TypedTuple<Object> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn("아이폰");
        when(tuple.getScore()).thenReturn(10.0);

        Set<ZSetOperations.TypedTuple<Object>> tuples = new LinkedHashSet<>();
        tuples.add(tuple);

        when(zSetOperations.reverseRangeWithScores(eq(dailyKey), eq(0L), eq(9L)))
                .thenReturn(tuples);

        //when
        searchCacheService.snapshotTop10toDb();

        //then
        verify(searchRankingSnapshotService).upsertSnapshot(eq("아이폰"), eq(10L));
    }
    @Test
    @DisplayName("snapshotTop10toDb - 데이터 없으면 저장 안 함")
    void snapshotTop10toDb_데이터없음() {
        // Given
        subZSet();
        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        when(zSetOperations.reverseRangeWithScores(eq(dailyKey), eq(0L), eq(9L)))
                .thenReturn(null);

        // When
        searchCacheService.snapshotTop10toDb();

        // Then
        verifyNoInteractions(searchRankingSnapshotService);
    }

    // ===================== cleanDBSearchLogs =====================

    @Test
    @DisplayName("cleanDBSearchLogs - 7일 전 로그 삭제")
    void cleanDBSearchLogs_정상() {
        //given

        // When
        searchCacheService.cleanDBSearchLogs();

        // Then
        verify(searchRepository).deletedOldLogs(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getRealtimeTop10 - count 0이면 키워드 삭제")
    void getRealtimeTop10_count_zero() {
        // Given
        subZSet();
        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        Set<Object> activeKeywords = new LinkedHashSet<>();
        activeKeywords.add("아이폰");

        when(zSetOperations.rangeByScore(eq(REALTIME_ACTIVE_KEY), anyDouble(), anyDouble()))
                .thenReturn(activeKeywords);

        // count = 0 → 삭제 분기!
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble()))
                .thenReturn(0L);

        // fallback용
        Set<Object> dailySet = new LinkedHashSet<>();
        dailySet.add("갤럭시");
        when(zSetOperations.reverseRange(eq(dailyKey), eq(0L), eq(9L)))
                .thenReturn(dailySet);

        // When
        List<String> result = searchCacheService.getRealtimeTop10();

        // Then (삭제 호출 확인)
        verify(redisTemplate).delete(eq(REALTIME_PREFIX + "아이폰"));
        verify(zSetOperations).remove(eq(REALTIME_ACTIVE_KEY), eq("아이폰"));
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("getRealtimeTop10 - 결과 비어있으면 fallback")
    void getRealtimeTop10_empty_fallback() {
        // Given
        subZSet();
        String dailyKey = DAILY_RANKING_PREFIX +
                LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        // 활성 키워드 6개 (5개 이상이지만 count가 null)
        Set<Object> activeKeywords = new LinkedHashSet<>();
        activeKeywords.add("아이폰");
        activeKeywords.add("맥북");
        activeKeywords.add("에어팟");
        activeKeywords.add("갤럭시");
        activeKeywords.add("노트북");
        activeKeywords.add("패드");

        when(zSetOperations.rangeByScore(eq(REALTIME_ACTIVE_KEY), anyDouble(), anyDouble()))
                .thenReturn(activeKeywords);

        // count > 0 → keywordCounts에 추가됨 → TOP10 정상 반환
        when(zSetOperations.count(anyString(), anyDouble(), anyDouble()))
                .thenReturn(5L);

        // When
        List<String> result = searchCacheService.getRealtimeTop10();

        // Then
        assertFalse(result.isEmpty());
        assertTrue(result.size() <= 10);
    }
}