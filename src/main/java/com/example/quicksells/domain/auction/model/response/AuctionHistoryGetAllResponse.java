package com.example.quicksells.domain.auction.model.response;

import com.example.quicksells.domain.auction.entity.AuctionHistory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class AuctionHistoryGetAllResponse {

    private final Long Id;
    private final Long auctionId;
    private final Long buyerId;
    private final Integer bidPrice;
    private final LocalDateTime updatedAt;

    public static AuctionHistoryGetAllResponse from(AuctionHistory auctionHistory) {
        return new AuctionHistoryGetAllResponse(
                auctionHistory.getId(),
                auctionHistory.getAuction().getId(),
                auctionHistory.getBuyer().getId(),
                auctionHistory.getBidPrice(),
                auctionHistory.getUpdatedAt()
        );
    }
}
