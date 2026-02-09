package com.example.quicksells.domain.appraise.controller;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.domain.appraise.model.request.AppraiseAuctionProceedRequest;
import com.example.quicksells.domain.appraise.model.request.AppraiseUpdateRequest;
import com.example.quicksells.domain.appraise.model.response.*;
import com.example.quicksells.domain.appraise.service.AppraiseService;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class AppraiseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppraiseService appraiseService;

    @InjectMocks
    private AppraiseController appraiseController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthUser authUser;
    private AuthUser authAdmin;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "user@test.com", UserRole.USER, "홍길동");
        authAdmin = new AuthUser(2L, "admin@test.com", UserRole.ADMIN, "관리자");

        mockMvc = MockMvcBuilders.standaloneSetup(appraiseController)
                .setCustomArgumentResolvers(
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().equals(AuthUser.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return authUser;
                            }
                        },
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("상품별 감정 목록 조회 성공")
    void getAppraises_Success() throws Exception {
        // given
        Long itemId = 1L;

        AppraiseGetAllResponse response1 = new AppraiseGetAllResponse(1L, 2L, "admin1", 1L, "테스트 상품", 100000, false, LocalDateTime.now());
        AppraiseGetAllResponse response2 = new AppraiseGetAllResponse(2L, 3L, "admin2", 1L, "테스트 상품", 120000, false, LocalDateTime.now());

        List<AppraiseGetAllResponse> responseList = List.of(response1, response2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<AppraiseGetAllResponse> pageResponse = new PageImpl<>(responseList, pageable, responseList.size());

        when(appraiseService.getAppraisesByItemId(eq(itemId), any(Pageable.class), any(AuthUser.class)))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/items/{itemId}/appraises", itemId)
                        .param("page", "0")
                        .param("size", "10")
                        .param("order", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품별 감정 전체 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.size").value(10))
                .andDo(print());

        verify(appraiseService, times(1)).getAppraisesByItemId(eq(itemId), any(Pageable.class), any(AuthUser.class));
    }


    @Test
    @DisplayName("상품별 감정 단건 조회 성공")
    void getAppraise_Success() throws Exception {
        // given
        Long itemId = 1L;
        Long appraiseId = 1L;

        AppraiseGetResponse response = new AppraiseGetResponse(appraiseId, 2L, "admin1", itemId, "테스트 상품", 100000, false, LocalDateTime.now());

        when(appraiseService.getAppraise(eq(appraiseId), eq(itemId), any(AuthUser.class))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/items/{itemId}/appraises/{id}", itemId, appraiseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("상품별 감정 단 건 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andDo(print());

        verify(appraiseService, times(1)).getAppraise(eq(appraiseId), eq(itemId), any(AuthUser.class));
    }

    @Test
    @DisplayName("감정 선택 성공")
    void updateAppraise_Success() throws Exception {
        // given
        Long appraiseId = 1L;
        AppraiseUpdateRequest request = new AppraiseUpdateRequest(true);

        AppraiseUpdateResponse response = new AppraiseUpdateResponse(
                appraiseId, 2L, "admin1", 1L, "테스트 상품",
                100000, true, LocalDateTime.now()
        );

        when(appraiseService.updateAppraise(eq(appraiseId), any(AppraiseUpdateRequest.class), any(AuthUser.class)))
                .thenReturn(response);

        // when & then
        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/appraises/{id}", appraiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("감정 선택에 성공했습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andDo(print());

        verify(appraiseService, times(1))
                .updateAppraise(eq(appraiseId), any(AppraiseUpdateRequest.class), any(AuthUser.class));
    }

    @Test
    @DisplayName("즉시 판매 확정 성공")
    void confirmImmediateSell_Success() throws Exception {
        // given
        Long appraiseId = 1L;

        AppraiseImmediateSellResponse response = new AppraiseImmediateSellResponse(appraiseId, 100000, AppraiseStatus.IMMEDIATE_SELL, 1L, StatusType.ON_SALE, 100000, 1L, "테스트 상품");

        when(appraiseService.confirmImmediateSell(eq(appraiseId)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/appraises/{id}/immediate-sell", appraiseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("선택한 감정가로 즉시 판매합니다."))
                .andExpect(jsonPath("$.data").exists())
                .andDo(print());

        verify(appraiseService, times(1)).confirmImmediateSell(eq(appraiseId));
    }

    @Test
    @DisplayName("경매 진행 확정 성공")
    void confirmAuctionWithCreate_Success() throws Exception {
        // given
        Long appraiseId = 1L;
        AppraiseAuctionProceedRequest request = new AppraiseAuctionProceedRequest(1);

        AppraiseAuctionProceedResponse response = new AppraiseAuctionProceedResponse(appraiseId, 100000, AppraiseStatus.AUCTION, 1L, "테스트 상품", 1L, 100000, LocalDateTime.now());

        when(appraiseService.confirmAuctionWithCreate(eq(appraiseId), eq(1)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/appraises/{id}/auction-proceed", appraiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("선택한 감정가로 경매 진행합니다."))
                .andExpect(jsonPath("$.data").exists())
                .andDo(print());

        verify(appraiseService, times(1)).confirmAuctionWithCreate(eq(appraiseId), eq(1));
    }
}