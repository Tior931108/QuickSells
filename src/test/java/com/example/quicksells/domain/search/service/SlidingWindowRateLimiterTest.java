package com.example.quicksells.domain.search.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlidingWindowRateLimiterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private SlidingWindowRateLimiter rateLimiter;

    private void subZSet() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }


    private final Long userId = 1L;
    private final int windowSeconds = 60;
    private final String key = "rate_limit:search:user:" + userId;
    private final int maxRequest = 5;

    // ===================== isAllowed =====================

    @Test
    @DisplayName("isAllowed - 제한 미만이면 true")
    void isAllowed_미만_true() {
        subZSet();
        Long userId = 1L;
        int windowSeconds = 60;
        String key = "rate_limit:search:user:" + userId;

        when(zSetOperations.count(eq(key), anyDouble(), anyDouble()))
                .thenReturn(3L);  // 3/5 → 통과

        boolean result = rateLimiter.isAllowed(userId, windowSeconds, maxRequest);

        assertTrue(result);
        verify(zSetOperations).add(eq(key), anyString(), anyDouble());
        verify(redisTemplate).expire(eq(key), eq(Duration.ofSeconds(windowSeconds + 60)));
    }

    @Test
    @DisplayName("isAllowed - 제한 초과면 false")
    void isAllowed_초과_false() {
        subZSet();
        when(zSetOperations.count(eq(key), anyDouble(), anyDouble()))
                .thenReturn(5L);  // 5/5 → 차단

        boolean result = rateLimiter.isAllowed(userId, windowSeconds, maxRequest);

        assertFalse(result);
        // 차단됐으니 add 호출 안 됨
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("isAllowed - count가 null이면 true")
    void isAllowed_count_null() {
        subZSet();
        when(zSetOperations.count(eq(key), anyDouble(), anyDouble()))
                .thenReturn(null);  // null → 0으로 처리

        boolean result = rateLimiter.isAllowed(userId, windowSeconds, maxRequest);

        assertTrue(result);
    }

    @Test
    @DisplayName("isAllowed - Redis 에러면 true (관대한 정책)")
    void isAllowed_에러_true() {
        subZSet();
        when(zSetOperations.count(eq(key), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Redis 에러"));

        boolean result = rateLimiter.isAllowed(userId, windowSeconds, maxRequest);

        assertTrue(result);
    }

    // ===================== getRemainingRequests =====================

    @Test
    @DisplayName("getRemainingRequests - 정상 계산")
    void getRemainingRequests_정상() {
        subZSet();
        when(zSetOperations.count(eq(key), anyDouble(), anyDouble()))
                .thenReturn(3L);  // 5 - 3 = 2

        int result = rateLimiter.getRemainingRequests(userId, windowSeconds, maxRequest);

        assertEquals(2, result);
    }

    @Test
    @DisplayName("getRemainingRequests - 초과 시 0 반환 (음수 방지)")
    void getRemainingRequests_초과_음수방지() {
        subZSet();
        when(zSetOperations.count(eq(key), anyDouble(), anyDouble()))
                .thenReturn(7L);  // 5 - 7 = -2 → 0

        int result = rateLimiter.getRemainingRequests(userId, windowSeconds, maxRequest);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("getRemainingRequests - count null이면 최대값")
    void getRemainingRequests_null() {
        subZSet();
        when(zSetOperations.count(eq(key), anyDouble(), anyDouble()))
                .thenReturn(null);  // null → 0으로 처리 → 5 - 0 = 5

        int result = rateLimiter.getRemainingRequests(userId, windowSeconds, maxRequest);

        assertEquals(5, result);
    }

    @Test
    @DisplayName("getRemainingRequests - Redis 에러 시 최대값")
    void getRemainingRequests_에러() {
        subZSet();
        when(zSetOperations.count(eq(key), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Redis 에러"));

        int result = rateLimiter.getRemainingRequests(userId, windowSeconds, maxRequest);

        assertEquals(maxRequest, result);
    }

    // ===================== getAfter =====================

    @Test
    @DisplayName("getAfter - 정상 (40초 후)")
    void getAfter_정상() {
        subZSet();
        long now = System.currentTimeMillis();
        long oldestTime = now - 20000;  // 20초 전

        Set<Object> set = new HashSet<>();
        set.add("request-1");

        when(zSetOperations.range(eq(key), anyLong(), anyLong()))
                .thenReturn(set);
        when(zSetOperations.score(eq(key), eq("request-1")))
                .thenReturn((double) oldestTime);

        long result = rateLimiter.getAfter(userId, windowSeconds);

        assertTrue(result >= 39 && result <= 41,
                "예상: 39~41초, 실제: " + result);
    }

    @Test
    @DisplayName("getAfter - 요청 없으면 0")
    void getAfterNotResponse() {
        subZSet();
        when(zSetOperations.range(eq(key), anyLong(), anyLong()))
                .thenReturn(Collections.emptySet());  // 빈 Set

        long result = rateLimiter.getAfter(userId, windowSeconds);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("getAfter - range null이면 0")
    void getAfter_null() {
        subZSet();
        when(zSetOperations.range(eq(key), anyLong(), anyLong()))
                .thenReturn(null);  // null 반환

        long result = rateLimiter.getAfter(userId, windowSeconds);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("getAfter - score null이면 0")
    void getAfter_score_null() {
        subZSet();
        Set<Object> set = new HashSet<>();
        set.add("request-1");

        when(zSetOperations.range(eq(key), anyLong(), anyLong()))
                .thenReturn(set);
        when(zSetOperations.score(eq(key), eq("request-1")))
                .thenReturn(null);  // score 못 가져옴

        long result = rateLimiter.getAfter(userId, windowSeconds);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("getAfter - 윈도우 이미 지났으면 0 (음수 방지)")
    void getAfter_window_no_minus() {
        subZSet();
        long now = System.currentTimeMillis();
        long oldestTime = now - 70000;  // 70초 전 (윈도우 60초)

        Set<Object> set = new HashSet<>();
        set.add("request-1");

        when(zSetOperations.range(eq(key), anyLong(), anyLong()))
                .thenReturn(set);
        when(zSetOperations.score(eq(key), eq("request-1")))
                .thenReturn((double) oldestTime);

        long result = rateLimiter.getAfter(userId, windowSeconds);

        assertEquals(0, result);  // 음수 → 0
    }

    @Test
    @DisplayName("getAfter - Redis 에러 시 0")
    void getAfter_error() {
        subZSet();
        when(zSetOperations.range(eq(key), anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Redis 에러"));

        long result = rateLimiter.getAfter(userId, windowSeconds);

        assertEquals(0, result);
    }

    // ===================== resetLimit =====================

    @Test
    @DisplayName("resetLimit - delete 호출 확인")
    void resetLimit_정상() {
        rateLimiter.resetLimit(userId);

        verify(redisTemplate, times(1)).delete(eq(key));
    }
}
