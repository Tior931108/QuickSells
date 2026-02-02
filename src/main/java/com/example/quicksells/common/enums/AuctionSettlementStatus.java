package com.example.quicksells.common.enums;

/**
 * 경매 정산 상태
 *
 * 정책:
 * - SUCCESSFUL_BID(낙찰)이라도 잔액 부족 등으로 정산이 실패할 수 있다.
 * - 이때 경매 결과(status)는 유지하고, settlementStatus로 운영 조치(추가 결제 유도 후 재정산)를 가능하게 한다.
 */
public enum AuctionSettlementStatus {
    PENDING,          // 정산 대기(낙찰 직후)
    COMPLETED,        // 정산 완료(buyer 차감 + seller 적립 + deal SOLD)
    PAYMENT_REQUIRED  // 잔액 부족 등으로 추가 결제 필요(운영 대응)
}
