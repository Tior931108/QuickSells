package com.example.quicksells.domain.payment.toss;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TossCancelResponse {

    /**
     * 취소된 결제의 paymentKey
     */
    private String paymentKey;

    /**
     * 토스 결제 상태 문자열 (예: CANCELED)
     */
    private String status;
}