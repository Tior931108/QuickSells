package com.example.quicksells.common.enums;

/**
 * v3 포인트 거래 유형
 *
 * - 정책: 사용자는 포인트를 충전한 금액만큼 보유
 * - 정책: 입찰은 보유 포인트 범위 내에서 가능하나, 낙찰 성공자만 포인트 차감
 * - 따라서 v3에서는 "충전"과 "낙찰 차감"만 존재한다.
 */
public enum PointTransactionType {

    /**
     * 포인트 충전
     * - 결제 승인 완료 후 포인트 증가와 함께 기록한다.
     */
    CHARGE,

    /**
     * 낙찰 성공으로 인한 포인트 차감
     * - 경매 종료 후 낙찰자에게만 포인트 차감을 적용하고 기록한다.
     */
    AUCTION_WIN_DEDUCT
}
