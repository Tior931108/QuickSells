package com.example.quicksells.domain.payment.model.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class PaymentConfirmResponse {

    /**
     * 내부 결제 PK
     */
    private final Long paymentId;

    /**
     * 주문번호(서버 발급)
     */
    private final String orderId;

    /**
     * 토스 결제 키
     */
    private final String paymentKey;

    /**
     * 결제 금액
     */
    private final Integer amount;

    /**
     * 승인 시각 (DB 저장값)
     */
    private final LocalDateTime approvedAt;

    /**
     * 충전 이후 지갑 잔액
     */
    private final Long walletBalance;
}