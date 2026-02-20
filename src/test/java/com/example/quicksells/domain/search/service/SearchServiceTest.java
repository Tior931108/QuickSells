package com.example.quicksells.domain.search.service;

import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.search.model.response.SearchGetResponse;
import com.example.quicksells.domain.search.repository.SearchCustomRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchCacheService searchCacheService;

    @Mock
    private SearchCustomRepositoryImpl searchCustomRepositoryImpl;

    @Mock
    private SlidingWindowRateLimiter slidingWindowRateLimiter;

    @InjectMocks
    private SearchService searchService;

    @Mock
    private AuthUser authUser;

    @Mock
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        lenient().when(authUser.getId()).thenReturn(1L);
    }
    @Test
    @DisplayName("search - 비로그인이면 예외")
    void search_authUser_null() {
        // When & Then
        assertThrows(CustomException.class, () ->
                searchService.search(null, "아이폰", null, null, pageable)
        );
    }

    @Test
    @DisplayName("search - Rate Limit 초과 예외")
    void search() {
        // Given
        when(slidingWindowRateLimiter.isAllowed(eq(1L), anyInt(), anyInt()))
                .thenReturn(false);
        when(slidingWindowRateLimiter.getRemainingRequests(eq(1L), anyInt(), anyInt()))
                .thenReturn(0);
        when(slidingWindowRateLimiter.getAfter(eq(1L), anyInt()))
                .thenReturn(10L);

        // When & Then
        assertThrows(CustomException.class, () ->
                searchService.search(authUser, "아이폰", null, null, pageable)
        );
    }

    @Test
    @DisplayName("search - keyword null이면 예외")
    void search_keyword_null() {
        // Given
        when(slidingWindowRateLimiter.isAllowed(eq(1L), anyInt(), anyInt()))
                .thenReturn(true);

        // When & Then
        assertThrows(CustomException.class, () ->
                searchService.search(authUser, null, null, null, pageable)
        );
    }
    @Test
    @DisplayName("search - keyword 공백이면 예외")
    void search_keyword_empty() {
        // Given
        when(slidingWindowRateLimiter.isAllowed(eq(1L), anyInt(), anyInt()))
                .thenReturn(true);

        // When & Then
        assertThrows(CustomException.class, () ->
                searchService.search(authUser, "   ", null, null, pageable)
        );
    }

    @Test
    @DisplayName("search - 일반 유저 정상 검색")
    void search_normal_user() {
        // Given
        Page<SearchGetResponse> mockPage = mock(Page.class);

        when(slidingWindowRateLimiter.isAllowed(eq(1L), anyInt(), anyInt()))
                .thenReturn(true);

        // USER 역할 설정
        when(authUser.getRole()).thenReturn(UserRole.USER);  // ← Role enum에 맞게 수정

        when(searchCustomRepositoryImpl.searchItems(
                anyString(), any(), any(), anyLong(), anyBoolean(), any()))
                .thenReturn(mockPage);

        // When
        Page<SearchGetResponse> result =
                searchService.search(authUser, "아이폰", null, null, pageable);

        // Then
        assertEquals(mockPage, result);
        verify(searchCacheService).notDoubleClick(eq("1"), eq("아이폰"));
    }

    @Test
    @DisplayName("search - 관리자 정상 검색")
    void search_admin_user() {
        // Given
        Page<SearchGetResponse> mockPage = mock(Page.class);

        when(slidingWindowRateLimiter.isAllowed(eq(1L), anyInt(), anyInt()))
                .thenReturn(true);

        // ADMIN 역할 설정
        when(authUser.getRole()).thenReturn(UserRole.ADMIN);  // ← Role enum에 맞게 수정

        when(searchCustomRepositoryImpl.searchItems(
                anyString(), any(), any(), anyLong(), anyBoolean(), any()))
                .thenReturn(mockPage);

        // When
        Page<SearchGetResponse> result =
                searchService.search(authUser, "아이폰", null, null, pageable);

        // Then
        assertEquals(mockPage, result);
        // isAdmin = true로 호출됐는지 확인
        verify(searchCustomRepositoryImpl).searchItems(
                anyString(), any(), any(), anyLong(), eq(true), any()
        );
    }

    // ===================== checkRateLimit =====================

    @Test
    @DisplayName("checkRateLimit - 허용이면 통과")
    void checkRateLimit_allowed() {
        // Given
        when(slidingWindowRateLimiter.isAllowed(eq(1L), anyInt(), anyInt()))
                .thenReturn(true);

        // When & Then (예외 없이 통과)
        assertDoesNotThrow(() -> searchService.checkRateLimit(1L));
    }

    @Test
    @DisplayName("checkRateLimit - 초과이면 예외")
    void checkRateLimit_exceeded() {
        // Given
        when(slidingWindowRateLimiter.isAllowed(eq(1L), anyInt(), anyInt()))
                .thenReturn(false);
        when(slidingWindowRateLimiter.getRemainingRequests(eq(1L), anyInt(), anyInt()))
                .thenReturn(0);
        when(slidingWindowRateLimiter.getAfter(eq(1L), anyInt()))
                .thenReturn(10L);

        // When & Then
        assertThrows(CustomException.class, () ->
                searchService.checkRateLimit(1L)
        );

        // 나머지 횟수, 재시도 시간 호출 확인
        verify(slidingWindowRateLimiter).getRemainingRequests(eq(1L), anyInt(), anyInt());
        verify(slidingWindowRateLimiter).getAfter(eq(1L), anyInt());
    }
}