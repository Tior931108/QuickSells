package com.example.quicksells.domain.deal.model.response;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.common.enums.AuctionStatusType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class DealCompletedResponse {

    private final Long dealId;
    private final Integer dealPrice;

    private final AppraiseStatus appraiseStatus;
    private final AuctionStatusType auctionStatus; // nullable

    private final Long itemId;
    private final String itemName;

    private final LocalDateTime completedAt; // 지금은 deal.createdAt 사용(완료시각 컬럼 없으니)
}
