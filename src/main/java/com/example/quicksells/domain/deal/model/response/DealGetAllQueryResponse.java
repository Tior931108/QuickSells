package com.example.quicksells.domain.deal.model.response;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.common.enums.StatusType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class DealGetAllQueryResponse {

    private final Long dealId;
    private final Integer dealPrice;
    private final StatusType dealStatus;
    private final LocalDateTime createdAt;

    private final AppraiseStatus appraiseStatus;
    private final AuctionStatusType auctionStatus; // nullable

    // item
    private final Long itemId;
    private final String itemTitle;

    // seller
    private final Long sellerId;
    private final String sellerName;

    // buyer (nullable)
    private final Long buyerId;
    private final String buyerName;
}
