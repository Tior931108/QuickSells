package com.example.quicksells.domain.auction.model.response;

import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.domain.auction.entity.Auction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class AuctionGetAllResponse {

    private final Long id;
    private final Long appraiseId;
    private final String itemName;
    private final Long buyerId;
    private final Integer bidPrice;
    private final AuctionStatusType status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime endTime;

    public static AuctionGetAllResponse from(Auction auction) {
        return new AuctionGetAllResponse(// 감정된 아이템 이름
                auction.getId(),
                auction.getAppraise().getId(),
                auction.getAppraise().getItem().getName(),
                auction.getBuyer() != null ? auction.getBuyer().getId() : null,// 입찰자가 존재하지 않으면 널을 반환
                auction.getBidPrice(),
                auction.getStatus(),
                auction.getCreatedAt(),
                auction.getUpdatedAt(),
                auction.getEndTime()
        );
    }
}
