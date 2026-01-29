package com.example.quicksells.domain.payment.toss;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TossConfirmResponse {

    /**
     * 토스 결제 키
     * - 우리 DB payment.paymentKey로 저장해야 함
     */
    private String paymentKey;

    /**
     * 주문번호(서버 발급)
     */
    private String orderId;

    /**
     * 결제 금액
     * - 토스는 totalAmount로 내려오는 경우가 흔하다고 하네용.
     */
    private Integer totalAmount;

    /**
     * 토스 결제 상태 문자열 (예: DONE)
     * - 지금은 최소 필드만 받음
     */
    private String status;
}