package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.payment.entity.Payment;
import com.example.quicksells.domain.payment.entity.PointTransaction;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.model.request.PaymentConfirmRequest;
import com.example.quicksells.domain.payment.model.request.PaymentOrderCreateRequest;
import com.example.quicksells.domain.payment.model.response.PaymentConfirmResponse;
import com.example.quicksells.domain.payment.model.response.PaymentOrderCreateResponse;
import com.example.quicksells.domain.payment.repository.PaymentRepository;
import com.example.quicksells.domain.payment.repository.PointTransactionRepository;
import com.example.quicksells.domain.payment.repository.PointWalletRepository;
import com.example.quicksells.domain.payment.toss.TossConfirmRequest;
import com.example.quicksells.domain.payment.toss.TossConfirmResponse;
import com.example.quicksells.domain.payment.toss.TossPaymentsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final TossPaymentsClient tossPaymentsClient;

    /**
     * 주문 생성(READY)
     *
     *  목적
     * - 백엔드가 orderId를 발급/저장해둬야 confirm 단계에서 아래 검증이 가능함:
     *   1) orderId 존재 여부
     *   2) 결제 요청 유저(owner) 검증
     *   3) amount 위/변조 방지 (READY 저장 amount vs confirm amount)
     */
    @Transactional
    public PaymentOrderCreateResponse createOrder(Long userId, PaymentOrderCreateRequest request) {

        // 1) 충전 정책 검증 (1만이상부터 only)
        validateChargeAmount(request.getAmount());

        // 2) 주문번호 생성 (유니크)
        String orderId = generateOrderId();

        // 3) Payment READY 생성/저장
        Payment payment = new Payment(userId, orderId, request.getAmount());
        paymentRepository.save(payment);

        return new PaymentOrderCreateResponse(orderId, request.getAmount()); // 정적메서드 처리 필요
    }

    /**
     * 결제 승인(confirm)
     *
     *  처리 흐름
     * 1) orderId 기반 READY 결제건 조회
     * 2) userId 소유권 검증
     * 3) amount 위/변조 검증
     * 4) paymentKey 중복(멱등성) 검증
     * 5) 토스 confirm 호출 (외부 승인)
     * 6) 내부 DB 반영 (Payment APPROVED, Wallet 충전, Transaction 기록)
     * 7) 내부 DB 반영 실패 시 → 토스 cancel 호출로 외부 결제 롤백
     */
    @Transactional
    public PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request) {

        validateChargeAmount(request.getAmount());

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_PAYMENT));

        if (!payment.getUserId().equals(userId)) {
            throw new CustomException(ExceptionCode.INVALID_USER_ROLE);
        }

        if (!payment.getAmount().equals(request.getAmount())) {
            payment.markAsFailed("금액 불일치 (변조 간으성 있음) - ready=" + payment.getAmount() + ", request=" + request.getAmount());
            paymentRepository.save(payment);
            throw new CustomException(ExceptionCode.INVALID_PAYMENT_AMOUNT);
        }

        if (paymentRepository.existsByPaymentKey(request.getPaymentKey())) {
            payment.markAsFailed("중복 결제 키 =" + request.getPaymentKey());
            paymentRepository.save(payment);
            throw new CustomException(ExceptionCode.DUPLICATE_PAYMENT_KEY);
        }

        // 1) 토스 승인(외부 승인)
        final TossConfirmResponse tossResponse;
        try {
            tossResponse = tossPaymentsClient.confirm(new TossConfirmRequest(request.getPaymentKey(), request.getOrderId(), request.getAmount()));
        } catch (Exception e) {
            // 토스 승인 자체가 실패 → 결제건 FAILED + 사유 기록
            payment.markAsFailed("toss confirm failed: " + safeMessage(e));
            paymentRepository.save(payment);
            throw new CustomException(ExceptionCode.TOSS_CONFIRM_FAILED);
        }

        // 2) 내부 DB 반영
        try {
            payment.markAsApproved(tossResponse.getPaymentKey());
            Payment savedPayment = paymentRepository.save(payment);

            // 지갑 생성
            PointWallet wallet = pointWalletRepository.findById(userId)
                    .orElseGet(() -> pointWalletRepository.save(new PointWallet(userId)));

            // 잔액 충전
            wallet.increaseBalance(savedPayment.getAmount().longValue());
            PointWallet savedWallet = pointWalletRepository.save(wallet);

            PointTransaction tx = new PointTransaction(
                    userId,
                    PointTransactionType.CHARGE,
                    savedPayment.getAmount().longValue(),
                    savedPayment.getId(),
                    null
            );
            pointTransactionRepository.save(tx);

            return new PaymentConfirmResponse(
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getPaymentKey(),
                    savedPayment.getAmount(),
                    savedPayment.getApprovedAt(),
                    savedWallet.getAvailableBalance()
            ); // 정적 메서드 처리 필요

        } catch (Exception dbException) {
            /**
             *  핵심: 토스 승인은 이미 성공(돈이 이동했을 수 있음)
             * 내부 DB 처리 실패 시 → 토스 cancel로 외부 결제 롤백을 반드시 시도해야 함
             */
            try {
                tossPaymentsClient.cancel(tossResponse.getPaymentKey(), "서버 처리 실패로 자동 취소");

                // cancel 성공: 우리 DB에 실패 사유 남김 (추적용)
                payment.markAsFailed("db failed after confirm; cancel success: " + safeMessage(dbException));
                paymentRepository.save(payment);

                // “롤백 성공이지만 내부 처리 실패”로 간주 → 에러 응답으로 통일
                throw new CustomException(ExceptionCode.TOSS_CANCEL_FAILED);

            } catch (CustomException ce) {
                // 위에서 의도적으로 던진 CustomException은 그대로 전달
                throw ce;

            } catch (Exception cancelException) {
                // cancel까지 실패하면 위험도가 높음: 결제는 승인됐는데 DB도 실패, 취소도 실패
                payment.markAsFailed("DB반영 및 cancel 실패 : "
                        + safeMessage(dbException) + " / cancelErr=" + safeMessage(cancelException));
                paymentRepository.save(payment);

                throw new CustomException(ExceptionCode.TOSS_CANCEL_FAILED);
            }
        }
    }

    /**
     * 예외 메시지 안전 추출
     * - Null 방지
     * - 너무 상세한 스택/민감정보는 남기지 않고 message 중심으로 기록
     */
    private String safeMessage(Exception e) {
        if (e == null) return "null";
        String msg = e.getMessage();
        return (msg == null || msg.isBlank()) ? e.getClass().getSimpleName() : msg;
    }

    /**
     * 충전 금액 정책 검증
     *
     *  정책
     * - 최소 10,000원 이상
     * - 단위 제한 없음
     */
    private void validateChargeAmount(Integer amount) {
        if (amount == null || amount < 10000) {
            throw new CustomException(ExceptionCode.INVALID_CHARGE_AMOUNT);
        }
    }

    /**
     * 주문번호 생성
     *
     *  요구사항
     * - 유니크해야 함
     * - 외부 결제 연동 시 안전한 문자열 사용
     */
    private String generateOrderId() {
        return "QS_" + UUID.randomUUID().toString().replace("-", "");
    }
}