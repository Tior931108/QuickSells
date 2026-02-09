package com.example.quicksells.domain.deal.controller;

import com.example.quicksells.common.enums.DealType;
import com.example.quicksells.common.model.PageResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.deal.model.request.DealCreateRequest;
import com.example.quicksells.domain.deal.model.response.DealCompletedResponse;
import com.example.quicksells.domain.deal.model.response.DealCreateResponse;
import com.example.quicksells.domain.deal.model.response.DealGetAllQueryResponse;
import com.example.quicksells.domain.deal.model.response.DealGetResponse;
import com.example.quicksells.domain.deal.service.DealService;
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

import static com.example.quicksells.common.enums.AppraiseStatus.PENDING;
import static com.example.quicksells.common.enums.AuctionStatusType.AUCTIONING;
import static com.example.quicksells.common.enums.StatusType.ON_SALE;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DealControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DealService dealService;

    @InjectMocks
    private DealController dealController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(2L, "user@test.com", USER, "유저");

        mockMvc = MockMvcBuilders.standaloneSetup(dealController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
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
                        }
                )
                .build();
    }

    @Test
    @DisplayName("거래 생성 성공 - POST /api/deals")
    void createDeal_success() throws Exception {
        DealCreateRequest req = new DealCreateRequest(10L, 30000);
        DealCreateResponse res = new DealCreateResponse(100L, 30000);

        when(dealService.createDeal(any(DealCreateRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/deals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("거래가 생성되었습니다."))
                .andExpect(jsonPath("$.data.dealId").value(100))
                .andExpect(jsonPath("$.data.dealPrice").value(30000));
    }

    @Test
    @DisplayName("거래 상세 조회 성공 - GET /api/deals/{id}")
    void getDealDetail_success() throws Exception {
        Long dealId = 100L;

        DealGetResponse res = new DealGetResponse(
                100L, ON_SALE, 30000, LocalDateTime.now(),
                PENDING, null
        );

        when(dealService.getDealDetail(eq(dealId), any(AuthUser.class))).thenReturn(res);

        mockMvc.perform(get("/api/deals/{id}", dealId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("거래 상세 조회를 성공했습니다"))
                .andExpect(jsonPath("$.data.dealId").value(100))
                .andExpect(jsonPath("$.data.dealPrice").value(30000));
    }

    @Test
    @DisplayName("거래 목록 조회 성공 - GET /api/deals?type=PURCHASE&page=0&size=10")
    void getDeals_success() throws Exception {
        DealGetAllQueryResponse row = new DealGetAllQueryResponse(
                100L,
                30000,
                ON_SALE,
                LocalDateTime.now(),
                PENDING,
                AUCTIONING,
                11L,
                "아이템명",
                2L,
                "판매자",
                3L,
                "구매자"
        );

        Page<DealGetAllQueryResponse> page = new PageImpl<>(
                List.of(row),
                PageRequest.of(0, 10),
                1
        );

        when(dealService.getDeals(eq(DealType.PURCHASE), any(AuthUser.class), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/deals")
                        .param("type", "PURCHASE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("거래 목록 조회를 성공하였습니다."))
                // PageResponse 구조: data.content, totalElements, totalPages, size, number
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.content[0].dealId").value(100))
                .andExpect(jsonPath("$.data.content[0].itemId").value(11))
                .andExpect(jsonPath("$.data.content[0].sellerName").value("판매자"));
    }

    @Test
    @DisplayName("판매 완료 거래 조회 성공 - GET /api/deals/completed?limit=10")
    void getCompletedDeals_success() throws Exception {
        List<DealCompletedResponse> res = List.of(
                new DealCompletedResponse(100L, 30000, PENDING, null, 11L, "아이템명", LocalDateTime.now())
        );

        when(dealService.getCompletedDeals(10)).thenReturn(res);

        mockMvc.perform(get("/api/deals/completed")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("판매 완료된 거래 목록 조회를 성공하였습니다."))
                .andExpect(jsonPath("$.data[0].dealId").value(100))
                .andExpect(jsonPath("$.data[0].dealPrice").value(30000));
    }
}
