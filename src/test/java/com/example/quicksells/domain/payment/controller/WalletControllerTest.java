package com.example.quicksells.domain.payment.controller;

import com.example.quicksells.common.exception.GlobalExceptionHandler;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.payment.model.request.AdminPointGrantRequest;
import com.example.quicksells.domain.payment.model.request.WithdrawRequest;
import com.example.quicksells.domain.payment.model.response.AdminPointGrantResponse;
import com.example.quicksells.domain.payment.model.response.PointTransactionGetResponse;
import com.example.quicksells.domain.payment.model.response.WalletGetResponse;
import com.example.quicksells.domain.payment.model.response.WithdrawResponse;
import com.example.quicksells.domain.payment.service.PointWalletService;
import com.example.quicksells.domain.payment.service.WithdrawalService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private MockMvc mockMvc;

    @Mock private WithdrawalService withdrawalService;
    @Mock private PointWalletService pointWalletService;

    @InjectMocks private WalletController walletController;


    private AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(2L, "user@test.com", USER, "유저");

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
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
                })
                .build();
    }

    @Test
    @DisplayName("출금 성공 - POST /api/wallets/withdrawals")
    void withdraw_success() throws Exception {
        String body = """
                {"amount": 10000}
                """;

        WithdrawResponse response = new WithdrawResponse(10_000L, 40_000L);
        when(withdrawalService.withdraw(eq(authUser), eq(10_000L))).thenReturn(response);

        mockMvc.perform(post("/api/wallets/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("출금이 완료되었습니다."))
                .andExpect(jsonPath("$.data.amount").value(10000))
                .andExpect(jsonPath("$.data.walletBalance").value(40000));
    }

    @Test
    @DisplayName("출금 실패 - @Min 위반 400")
    void withdraw_validation_400() throws Exception {
        String body = """
                {"amount": 0}
                """;

        mockMvc.perform(post("/api/wallets/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("출금 금액은 1원 이상이어야 합니다."));
    }

    @Test
    @DisplayName("내 지갑 조회 성공 - GET /api/wallets/me")
    void getMyWallet_success() throws Exception {
        WalletGetResponse response = new WalletGetResponse(authUser.getId(), 55_000L);
        when(pointWalletService.getMyWalletResponse(authUser.getId())).thenReturn(response);

        mockMvc.perform(get("/api/wallets/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 지갑 조회를 성공하였습니다."))
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.availableBalance").value(55000));
    }

    @Test
    @DisplayName("내 거래내역 조회 성공 - GET /api/wallets/transactions?page=0&size=20")
    void getMyTransactions_success() throws Exception {
        PointTransactionGetResponse tx = new PointTransactionGetResponse(
                1L, null, 10_000L, "CREDIT", null, null, null, null
        );
        Page<PointTransactionGetResponse> page = new PageImpl<>(
                List.of(tx),
                PageRequest.of(0, 20),
                1
        );

        when(pointWalletService.getMyTransactionsResponse(eq(authUser.getId()), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/wallets/transactions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 포인트 거래내역 조회를 성공하였습니다."))
                // PageResponse.data.*
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].amount").value(10000))
                .andExpect(jsonPath("$.data.content[0].direction").value("CREDIT"));
    }

    @Test
    @DisplayName("관리자 포인트 지급 성공 - POST /api/admin/wallets/{userId}/grant")
    void grantPoint_success() throws Exception {
        long targetUserId = 5L;

        String body = """
                {"amount": 20000}
                """;

        AdminPointGrantResponse response = new AdminPointGrantResponse(targetUserId, 20_000L, 80_000L);
        when(pointWalletService.grantPointResponse(eq(targetUserId), eq(20_000L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/wallets/{userId}/grant", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("관리자 포인트 지급에 성공하였습니다."))
                .andExpect(jsonPath("$.data.userId").value(5))
                .andExpect(jsonPath("$.data.amount").value(20000))
                .andExpect(jsonPath("$.data.walletBalance").value(80000));
    }

    @Test
    @DisplayName("관리자 포인트 지급 실패 - @Min 위반 400")
    void grantPoint_validation_400() throws Exception {
        String body = """
                {"amount": 0}
                """;

        mockMvc.perform(post("/api/admin/wallets/{userId}/grant", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("지급 금액은 1원 이상이어야 합니다."));
    }

    @Test
    @DisplayName("관리자 특정 유저 지갑 조회 성공 - GET /api/admin/wallets/{userId}")
    void getUserWallet_success() throws Exception {
        WalletGetResponse response = new WalletGetResponse(5L, 12345L);
        when(pointWalletService.getUserWalletResponse(5L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/wallets/{userId}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("유저 지갑 조회를 성공하였습니다."))
                .andExpect(jsonPath("$.data.userId").value(5))
                .andExpect(jsonPath("$.data.availableBalance").value(12345));
    }

    @Test
    @DisplayName("관리자 특정 유저 거래내역 조회 성공 - GET /api/admin/wallets/{userId}/transactions")
    void getUserTransactions_success() throws Exception {
        long userId = 5L;

        PointTransactionGetResponse tx = new PointTransactionGetResponse(
                10L, null, 50_000L, "DEBIT", null, 7L, 100L, null
        );
        Page<PointTransactionGetResponse> page = new PageImpl<>(
                List.of(tx),
                PageRequest.of(1, 5),
                11
        );

        when(pointWalletService.getUserTransactionsResponse(eq(userId), eq(1), eq(5)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/wallets/{userId}/transactions", userId)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("유저 거래내역 조회를 성공하였습니다."))
                .andExpect(jsonPath("$.data.totalElements").value(11))
                .andExpect(jsonPath("$.data.totalPages").value(3)) // ceil(11/5)=3
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(10))
                .andExpect(jsonPath("$.data.content[0].amount").value(50000))
                .andExpect(jsonPath("$.data.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.data.content[0].auctionId").value(7))
                .andExpect(jsonPath("$.data.content[0].dealId").value(100));
    }
}
