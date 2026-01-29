package com.example.quicksells.domain.payment.toss;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TossConfirmRequest {

    /**
     * 토스가 제공하는 결제 고유키
     * - successUrl 쿼리로 내려옴
     */
    private String paymentKey;

    /**
     * 주문번호 (서버가 생성한 값)
     * - successUrl 쿼리로 내려옴
     */
    private String orderId;

    /**
     * 결제 금액
     * - successUrl 쿼리로 내려옴
     */
    private Integer amount;
}