package com.example.quicksells.domain.payment.entity;

import com.example.quicksells.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "payments",
        uniqueConstraints = {
                /**
                 * orderId는 "서버가 발급"하는 주문번호
                 * - 서버에서 생성 → 토스 결제 요청/승인 시 모두 이 값으로 결제건을 추적함
                 * - 중복되면 결제건이 섞이므로 반드시 유니크 보장
                 */
                @UniqueConstraint(name = "unique_payment_order_id", columnNames = "order_id"),

                /**
                 * paymentKey는 "토스가 승인 성공 후 발급"하는 결제 고유키
                 * - 결제 중복 승인/중복 처리 방지를 위해 유니크 보장
                 * - READY 상태에서는 paymentKey가 아직 없으므로 NULL 허용
                 */
                @UniqueConstraint(name = "unique_payment_key", columnNames = "payment_key")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    /**
     * 내부 결제 PK
     * - 우리 시스템에서 결제 레코드를 식별하는 키
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 결제를 요청한 사용자 ID
     * - 결제/포인트는 반드시 user 단위로 귀속됨
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 주문번호(orderId)
     * - 서버가 생성(READY 단계)
     * - 토스 결제 요청/승인(confirm) 시 반드시 이 값을 함께 사용
     */
    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    /**
     * 토스 결제 키(paymentKey)
     * - 토스 승인(confirm) 성공 후 발급됨
     * - READY 상태에서는 null이어야 정상
     */
    @Column(name = "payment_key", nullable = true, length = 200)
    private String paymentKey;

    /**
     * 결제 금액 (원 단위)
     * - READY 단계에서 저장해두고 confirm 요청 값과 비교하여 위/변조 방지에 사용
     */
    @Column(name = "amount", nullable = false)
    private Integer amount;

    /**
     * 결제 상태
     * - READY -> APPROVED 또는 FAILED 로 전이
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    /**
     * 토스 승인 시각
     * - READY 단계에서는 null
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 결제 생성 시각
     * - READY 생성 시점 기록
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 실패 사유(추적)
     * - 실패 케이스(토스 승인 실패, 내부 DB 처리 실패, 롤백 실패 등)를 문자열로 기록
     * - 너무 길어지지 않게 제한
     */
    @Column(name = "fail_reason", length = 300)
    private String failReason;

    /**
     * 주문 생성(READY)
     *
     *  포인트 충전 설계 흐름
     * 1) 서버가 orderId 생성
     * 2) Payment를 READY 상태로 저장
     * 3) 결제 성공 리다이렉트(sucessUrl)에서 paymentKey/orderId/amount로 confirm 호출
     */
    public Payment(Long userId, String orderId, Integer amount) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentStatus = PaymentStatus.READY;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 승인 완료 상태로 전환
     * - 토스 confirm 성공 이후 호출
     */
    public void markAsApproved(String paymentKey) {
        this.paymentKey = paymentKey;
        this.paymentStatus = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();

        // 승인 성공했으니 실패 사유는 비워둠
        this.failReason = null;
    }

    /**
     * 실패 처리 + 사유 기록
     * - 운영/시연 시 어떤 이유로 실패했는지 남기기 위함
     */
    public void markAsFailed(String reason) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.failReason = truncate(reason, 300);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}