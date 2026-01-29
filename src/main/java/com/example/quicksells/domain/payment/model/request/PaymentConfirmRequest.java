package com.example.quicksells.domain.payment.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentConfirmRequest {

    /**
     * 토스 결제 키
     * - successUrl 쿼리로 전달받음
     */
    @NotBlank
    private String paymentKey;

    /**
     * 주문번호(서버 발급)
     * - successUrl 쿼리로 전달받음
     */
    @NotBlank
    private String orderId;

    /**
     * 결제 금액
     * - successUrl 쿼리로 전달받음
     */
    @NotNull
    @Min(1)
    private Integer amount;
}