package com.example.quicksells.domain.payment.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * PointTransaction이 어떤 이벤트에 의해 발생했는지 추적하는 참조 값.
 *
 * 정책:
 * - 충전: paymentId 사용
 * - 경매 정산: auctionId 사용
 * - 즉시판매: dealId 사용
 * - 출금: 참조 없음
 */
@Getter
@RequiredArgsConstructor
public class TransactionReference {

    private final Long paymentId;
    private final Long auctionId;
    private final Long dealId;

    public static TransactionReference none() {
        return new TransactionReference(null, null, null);
    }

    public static TransactionReference ofPayment(Long paymentId) {
        return new TransactionReference(paymentId, null, null);
    }

    public static TransactionReference ofAuction(Long auctionId) {
        return new TransactionReference(null, auctionId, null);
    }

    public static TransactionReference ofDeal(Long dealId) {
        return new TransactionReference(null, null, dealId);
    }
}
