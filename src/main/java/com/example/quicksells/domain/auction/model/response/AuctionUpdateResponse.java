package com.example.quicksells.domain.auction.model.response;

import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.domain.auction.entity.Auction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class AuctionUpdateResponse {

    private final Long id;
    private final Long appraiseId;
    private final String itemName;
    private final Long buyerId;
    private final Integer bidPrice;
    private final AuctionStatusType status;
    private final LocalDateTime updatedAt;
    private final LocalDateTime endTime;

    public static AuctionUpdateResponse from(Auction auction) {
        return new AuctionUpdateResponse(
                auction.getId(),
                auction.getAppraise().getId(),
                auction.getAppraise().getItem().getName(),
                auction.getBuyer().getId(),
                auction.getBidPrice(),
                auction.getStatus(),
                auction.getUpdatedAt(),
                auction.getEndTime()
        );
    }
}
