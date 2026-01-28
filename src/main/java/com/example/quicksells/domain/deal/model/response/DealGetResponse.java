package com.example.quicksells.domain.deal.model.response;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.domain.deal.entity.Deal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class DealGetResponse {

    private final Long dealId;
    private final StatusType dealStatus;               // Deal.status (ON_SALE / SOLD)
    private final Integer dealPrice;
    private final LocalDateTime createdAt;

    // derived status
    private final AppraiseStatus appraiseStatus;       // Appraise.appraiseStatus (IMMEDIATE_SELL / AUCTION / PENDING)
    private final AuctionStatusType auctionStatus;     // Auction.status (nullable)

    public static DealGetResponse from(Deal deal) {
        return new DealGetResponse(
                deal.getId(),
                deal.getStatus(),
                deal.getDealPrice(),
                deal.getCreatedAt(),
                deal.getAppraise().getAppraiseStatus(),
                deal.getAuction() != null ? deal.getAuction().getStatus() : null
        );
    }
}
