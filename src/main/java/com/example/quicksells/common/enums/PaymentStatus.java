package com.example.quicksells.common.enums;

/**
 * 결제 상태 (포인트 충전 결제 기준)
 *
 * - READY    : 서버가 orderId를 발급해 "주문"만 만들어둔 상태 (아직 토스 승인 전)
 * - APPROVED : 토스 결제 승인(confirm)까지 완료된 상태 (포인트 충전 처리 가능)
 * - FAILED   : 승인 실패 또는 내부 처리 실패로 인해 결제가 최종 실패 처리된 상태
 *
 *   설계 이유
 * - 결제는 "외부(토스) 승인" + "내부(DB 반영)" 두 단계가 있음
 * - 외부 승인이 성공해도 내부 DB 반영이 실패할 수 있으므로 상태 추적이 필요
 */
public enum PaymentStatus {
    READY,
    APPROVED,
    FAILED
}
