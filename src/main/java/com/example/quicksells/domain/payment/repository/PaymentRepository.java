package com.example.quicksells.domain.payment.repository;

import com.example.quicksells.common.enums.PaymentStatus;
import com.example.quicksells.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * orderId는 서버가 생성하는 주문번호(유니크)
     * - 주문 생성 중복 방지/조회에 사용
     */
    boolean existsByOrderId(String orderId);

    List<Payment> findTop200ByPaymentStatusAndCreatedAtBeforeOrderByCreatedAtAsc(PaymentStatus status, LocalDateTime cutoff);

    /**
     *  중요: 메서드명은 엔티티 필드명(paymentKey)과 일치해야 함
     * - 기존에 existsByTossPaymentKey 같은 형태면 런타임에서 프로퍼티 못찾고 터짐
     */
    boolean existsByPaymentKey(String paymentKey);

    /**
     * confirm 단계에서 orderId로 READY 결제건을 가져오기 위한 조회
     */
    Optional<Payment> findByOrderId(String orderId);
}