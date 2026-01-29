package com.example.quicksells.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatRoomType {

    /**
     * 사용자 - 관리자(감정사) 채팅
     * - 제약 없음
     * - 언제든지 1 : 1 문의 가능
     */
    USER_ADMIN("사용자-관리자 채팅"),

    /**
     * 구매자 - 판매자 채팅
     * - 경매 낙찰 후에만 가능
     * - Appraise.appraiseStatus 타입 : AUCTION (경매)
     * - Auction.AuctionStatusType 타입 : SUCCESSFUL_BID (낙찰)
     */
    BUYER_SELLER("구매자-판매자 채팅");

    private final String description;
}
