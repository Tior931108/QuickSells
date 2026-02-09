package com.example.quicksells.common.enums;

/**
 * v3 포인트 거래 유형
 *
 * 정책:
 * - 충전(+) : 결제 승인 후 지갑 증가
 * - 낙찰 차감(-) : 경매 종료 후 낙찰자 지갑 차감
 * - 판매자 적립(+) : 낙찰 금액을 판매자 지갑에 적립
 * - 즉시 판매(+) : 즉시 판매로 인해 판매자 지갑에 적립
 * - 출금(-) : 갖고 있는 포인트 수 만큼 판매자 계좌로 출금
 */
public enum PointTransactionType {

    /**
     * 포인트 충전
     */
    CHARGE,

    /**
     * 낙찰 성공으로 인한 포인트 차감(구매자)
     */
    AUCTION_WIN_DEDUCT,

    /**
     * 낙찰 성공으로 인한 포인트 적립(판매자)
     */
    AUCTION_SELLER_CREDIT,

    /**
     * 회사가 즉시매입을 확정하여 판매자에게 포인트를 지급
     */
    IMMEDIATE_SELL_CREDIT,

    /**
     * 출금(내부 포인트 차감 + 거래내역 기록)
     */
    WITHDRAW,

    /**
     * 운영자(관리자) 지급 포인트
     * - CS/이벤트/보상 지급 등
     */
    ADMIN_GRANT
}
