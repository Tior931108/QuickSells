package com.example.quicksells.domain.payment.entity;

import com.example.quicksells.common.enums.PointTransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "point_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction {

    /**
     * 포인트 거래 내역 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 포인트 거래 대상 사용자 식별자
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 포인트 거래 유형 (v3: 충전 / 낙찰 차감)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private PointTransactionType transactionType;

    /**
     * 변동 포인트 수치
     *
     * - 충전: 양수
     * - 낙찰 차감: 음수로 기록할 수도 있지만,
     *   구현 혼란을 줄이기 위해 항상 "양수"로 받고 유형으로 의미를 구분해도 된다.
     *   (팀 정책에 맞춰 선택)
     */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /**
     * 결제 기반 충전이라면 결제 식별자 연결
     * - 충전이 아닌 경우 null
     */
    @Column(name = "payment_id")
    private Long paymentId;

    /**
     * 낙찰 차감이라면 경매 식별자 연결
     * - 충전인 경우 null
     */
    @Column(name = "auction_id")
    private Long auctionId;

    /**
     * 거래 생성 시각
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PointTransaction(Long userId, PointTransactionType transactionType, Long amount, Long paymentId, Long auctionId) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.paymentId = paymentId;
        this.auctionId = auctionId;
        this.createdAt = LocalDateTime.now();
    }
}