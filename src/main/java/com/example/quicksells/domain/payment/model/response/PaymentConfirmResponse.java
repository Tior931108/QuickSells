package com.example.quicksells.domain.payment.model.response;

import com.example.quicksells.domain.payment.entity.Payment;
import com.example.quicksells.domain.payment.entity.PointWallet;
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

    public static PaymentConfirmResponse from(Payment payment, PointWallet wallet) {
        return new PaymentConfirmResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getAmount(),
                payment.getApprovedAt(),
                wallet.getAvailableBalance()
        );
    }
}