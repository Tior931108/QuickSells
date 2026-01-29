package com.example.quicksells.common.scheduler;

import com.example.quicksells.common.enums.PaymentStatus;
import com.example.quicksells.domain.payment.entity.Payment;
import com.example.quicksells.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCleanupScheduler {

    private final PaymentRepository paymentRepository;

    /**
     * READY 결제건 정리 스케줄러
     *
     *  목적
     * - 결제창만 열고 이탈한 경우 READY가 계속 쌓임
     * - 일정 시간이 지난 READY는 실패 처리로 정리하여 운영 데이터 개선
     *
     *  주기
     * - 5분마다 실행 (필요 시 조정)
     *
     *  정책
     * - 30분 이상 지난 READY는 FAILED 처리
     */
    @Transactional
    @Scheduled(cron = "0 */5 * * * *") // 매 5분마다
    public void cleanupOldReadyPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        // 30분 이상 지난 READY 200건은 FAILED 처리 삭제하는 거 아님.
        List<Payment> targets = paymentRepository.findTop200ByPaymentStatusAndCreatedAtBeforeOrderByCreatedAtAsc(PaymentStatus.READY, cutoff);

        if (targets.isEmpty()) return;

        for (Payment p : targets) {
            p.markAsFailed("준비 시간이 초과되었습니다.(사용자 포기 결제 흐름)");
        }

        // dirty checking으로 업데이트 반영
        log.info("[PaymentCleanup] 에서 {} 결제를 실패로 표시 FAILED (READY timeout)", targets.size());
    }
}