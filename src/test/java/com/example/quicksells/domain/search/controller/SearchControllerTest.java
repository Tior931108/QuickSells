package com.example.quicksells.domain.search.controller;

import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.search.model.response.SearchGetResponse;
import com.example.quicksells.domain.search.service.SearchCacheService;
import com.example.quicksells.domain.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private SearchService searchService;
    @Mock
    private SearchCacheService searchCacheService;
    @InjectMocks
    private SearchController searchController;

    private AuthUser authUser;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "test@test.com", UserRole.USER,"테스트이름");

        mockMvc = MockMvcBuilders.standaloneSetup(searchController)
                .setCustomArgumentResolvers( new PageableHandlerMethodArgumentResolver(),new HandlerMethodArgumentResolver() {

                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return authUser;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("keywordGet - 검색 결과 있으면 200 + 검색 결과입니다.")
    void keywordGet_yes_item() throws Exception  {
        // Given
        Page<SearchGetResponse> mockPage = mock(Page.class);

        when(mockPage.isEmpty()).thenReturn(false);  // 결과 있음

        when(searchService.search(any(), eq("아이폰"), any(), any(), any()))
                .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/item/searches")
                        .param("keyword", "아이폰"))
                        .andExpect(status().isOk())
                        .andExpect((ResultMatcher) jsonPath("$.message").value("검색 결과입니다."));
    }

    @Test
    @DisplayName("keywordGet - 검색 결과 없으면 200 + 등록된 상품이 없습니다.")
    void keywordGet_not_item() throws Exception {
        // Given
        Page<SearchGetResponse> mockPage = mock(Page.class);

        when(mockPage.isEmpty()).thenReturn(true);  // 결과 없음

        when(searchService.search(any(), eq("없는상품"), any(), any(), any()))
                .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/item/searches")
                        .param("keyword", "없는상품"))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) jsonPath("$.message").value("등록된 상품이 없습니다."));
    }


    // ===================== getPopularRankings =====================

    @Test
    @DisplayName("getPopularRankings - 인기 검색어 TOP10 정상 반환")
    void getPopularRankings_return() throws Exception {
        // Given
        List<String> keywords = List.of("아이폰", "맥북", "에어팟");

        when(searchCacheService.getPopularKeywordsList()).thenReturn(keywords);

        // When & Then
        mockMvc.perform(get("/api/popular/searches"))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) jsonPath("$.message").value("인기 검색어 목록입니다."));
    }

    // ===================== getRealtimeTop10 =====================

    @Test
    @DisplayName("getRealtimeTop10 - 실시간 인기 검색어 정상 반환")
    void getRealtimeTop10_Keyword_return() throws Exception {
        // Given
        List<String> keywords = List.of("아이폰", "갤럭시");

        when(searchCacheService.getRealtimeTop10()).thenReturn(keywords);

        // When & Then
        mockMvc.perform(get("/api/popular/realtime"))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) jsonPath("$.message").value("인기 검색어 목록입니다."));
    }
}