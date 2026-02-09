package com.example.quicksells.domain.payment.controller;

import com.example.quicksells.common.exception.GlobalExceptionHandler;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.payment.model.request.PaymentConfirmRequest;
import com.example.quicksells.domain.payment.model.request.PaymentOrderCreateRequest;
import com.example.quicksells.domain.payment.model.response.PaymentConfirmResponse;
import com.example.quicksells.domain.payment.model.response.PaymentOrderCreateResponse;
import com.example.quicksells.domain.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.time.LocalDateTime;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(2L, "user@test.com", USER, "유저");

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // @Value("${toss.payments.client-key}") 필드 주입 대체
        ReflectionTestUtils.setField(paymentController, "tossClientKey", "test_client_key");

        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
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
    @DisplayName("주문 생성(READY) 성공 - POST /api/payments/orders")
    void createOrder_success() throws Exception {
        PaymentOrderCreateRequest req = new PaymentOrderCreateRequest(10000);
        PaymentOrderCreateResponse res = new PaymentOrderCreateResponse("QS_ORDER_1", 10000);

        when(paymentService.createOrder(eq(authUser.getId()), any(PaymentOrderCreateRequest.class)))
                .thenReturn(res);

        mockMvc.perform(post("/api/payments/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 생성을 성공했습니다."))
                .andExpect(jsonPath("$.data.orderId").value("QS_ORDER_1"))
                .andExpect(jsonPath("$.data.amount").value(10000));
    }

    @Test
    @DisplayName("주문 생성(READY) 실패 - @Min 위반 400")
    void createOrder_validation_400() throws Exception {
        PaymentOrderCreateRequest req = new PaymentOrderCreateRequest(5000);

        mockMvc.perform(post("/api/payments/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("최소 충전 금액은 10,000원입니다."));
    }

    @Test
    @DisplayName("결제 승인(confirm) 성공 - POST /api/payments/confirm")
    void confirm_success() throws Exception {
        PaymentConfirmRequest req = new PaymentConfirmRequest("payKey-1", "order-1", 10000);
        PaymentConfirmResponse res = new PaymentConfirmResponse(
                1L, "order-1", "payKey-1", 10000, LocalDateTime.now(), 50000L
        );

        when(paymentService.confirm(eq(authUser.getId()), any(PaymentConfirmRequest.class)))
                .thenReturn(res);

        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("결제 승인을 성공했습니다."))
                .andExpect(jsonPath("$.data.paymentId").value(1))
                .andExpect(jsonPath("$.data.orderId").value("order-1"))
                .andExpect(jsonPath("$.data.paymentKey").value("payKey-1"))
                .andExpect(jsonPath("$.data.amount").value(10000));
    }

    @Test
    @DisplayName("결제 승인(confirm) 실패 - @NotBlank 위반 400")
    void confirm_validation_400() throws Exception {
        PaymentConfirmRequest req = new PaymentConfirmRequest("", "", null);

        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("토스 clientKey 조회 성공 - GET /api/payments/config")
    void config_success() throws Exception {
        mockMvc.perform(get("/api/payments/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토스 설정 조회 성공!"))
                .andExpect(jsonPath("$.data.clientKey").value("test_client_key"));
    }
}
