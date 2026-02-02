package com.example.quicksells.domain.auction.model.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 관리자: 낙찰 정산 재시도 응답 DTO
 *
 * - auctionId: 재정산 대상 경매 ID
 * - dealId: 연결된 거래 ID
 * - settled: 정산 완료 여부
 */
@Getter
@RequiredArgsConstructor
public class AdminAuctionSettlementRetryResponse {

    private final Long auctionId;
    private final Long dealId;
    private final boolean settled;

    public static AdminAuctionSettlementRetryResponse from(Long auctionId, Long dealId, boolean settled) {
        return new AdminAuctionSettlementRetryResponse(auctionId, dealId, settled);
    }

    public static AdminAuctionSettlementRetryResponse from(Long auctionId, Long dealId) {
        return new AdminAuctionSettlementRetryResponse(auctionId, dealId, true);
    }
}
