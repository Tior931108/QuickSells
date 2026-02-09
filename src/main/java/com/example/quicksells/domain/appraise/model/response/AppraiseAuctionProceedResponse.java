package com.example.quicksells.domain.appraise.model.response;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.model.response.AuctionCreateResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class AppraiseAuctionProceedResponse {

    // 감정 정보
    private final Long appraiseId;
    private final Integer appraisedPrice;  // 시작가로 사용될 감정가
    private final AppraiseStatus appraiseStatus;  // 감정 선택 후 진행방식

    // 상품 정보
    private final Long itemId;
    private final String itemName;

    // 경매 정보
    private final Long auctionId;
    private final Integer bidPrice;
    private final LocalDateTime expectedAuctionStartTime;

    // 경매 생성 후 정보 추가
    public static AppraiseAuctionProceedResponse of(Appraise appraise, AuctionCreateResponse auction) {
        return new AppraiseAuctionProceedResponse(
                appraise.getId(),
                appraise.getBidPrice(),
                appraise.getAppraiseStatus(),
                appraise.getItem().getId(),
                appraise.getItem().getName(),
                auction.getId(),
                auction.getBidPrice(),
                auction.getCreatedAt()
        );

    }
}
