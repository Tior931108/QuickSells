package com.example.quicksells.domain.payment.model.response;

import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.domain.payment.entity.PointTransaction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class PointTransactionGetResponse {

    private final Long id;
    private final PointTransactionType transactionType;

    /**
     * amount는 "항상 양수" 정책(현재 프로젝트 정책 유지)
     */
    private final Long amount;

    /**
     * 사용자 화면 편의를 위한 방향(CREDIT/DEBIT)
     */
    private final String direction;

    private final Long paymentId;
    private final Long auctionId;
    private final Long dealId;
    private final LocalDateTime createdAt;



    public static PointTransactionGetResponse from(PointTransaction tx) {
        return new PointTransactionGetResponse(
                tx.getId(),
                tx.getTransactionType(),
                tx.getAmount(),
                resolveDirection(tx.getTransactionType()),
                tx.getPaymentId(),
                tx.getAuctionId(),
                tx.getDealId(),
                tx.getCreatedAt()
        );
    }

    private static String resolveDirection(PointTransactionType type) {
        if (type == PointTransactionType.AUCTION_WIN_DEDUCT || type == PointTransactionType.WITHDRAW) {
            return "DEBIT";
        }
        return "CREDIT";
    }
}
