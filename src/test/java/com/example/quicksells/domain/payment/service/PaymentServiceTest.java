package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.payment.entity.Payment;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.model.TransactionReference;
import com.example.quicksells.domain.payment.model.request.PaymentConfirmRequest;
import com.example.quicksells.domain.payment.model.request.PaymentOrderCreateRequest;
import com.example.quicksells.domain.payment.model.response.PaymentConfirmResponse;
import com.example.quicksells.domain.payment.model.response.PaymentOrderCreateResponse;
import com.example.quicksells.domain.payment.repository.PaymentRepository;
import com.example.quicksells.domain.payment.toss.TossConfirmRequest;
import com.example.quicksells.domain.payment.toss.TossConfirmResponse;
import com.example.quicksells.domain.payment.toss.TossPaymentsClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private TossPaymentsClient tossPaymentsClient;
    @Mock private PointLedgerService pointLedgerService;

    @InjectMocks private PaymentService paymentService;

    @Test
    @DisplayName("주문 생성(READY) 성공")
    void createOrder_success() {
        Long userId = 2L;
        PaymentOrderCreateRequest req = new PaymentOrderCreateRequest(10000);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentOrderCreateResponse res = paymentService.createOrder(userId, req);

        assertThat(res).isNotNull();
        assertThat(res.getAmount()).isEqualTo(10000);
        assertThat(res.getOrderId()).isNotBlank();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("주문 생성(READY) 실패 - 충전 금액 정책 위반(INVALID_CHARGE_AMOUNT)")
    void createOrder_fail_invalidChargeAmount() {
        Long userId = 2L;
        PaymentOrderCreateRequest req = new PaymentOrderCreateRequest(9999);

        assertThatThrownBy(() -> paymentService.createOrder(userId, req))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.INVALID_CHARGE_AMOUNT.getMessage());

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 승인 성공 - 토스 confirm 성공 + 내부 반영 성공")
    void confirm_success() {
        Long userId = 2L;
        Payment payment = new Payment(userId, "order-1", 10000);
        ReflectionTestUtils.setField(payment, "id", 1L);

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByPaymentKey("payKey-1")).thenReturn(false);

        when(tossPaymentsClient.confirm(any(TossConfirmRequest.class)))
                .thenReturn(new TossConfirmResponse("payKey-1", "order-1", 10000, "DONE"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PointWallet wallet = mock(PointWallet.class);
        when(wallet.getAvailableBalance()).thenReturn(50000L);

        when(pointLedgerService.credit(eq(userId), eq(10000L), eq(PointTransactionType.CHARGE), any(TransactionReference.class)))
                .thenReturn(wallet);

        PaymentConfirmResponse res = paymentService.confirm(userId, new PaymentConfirmRequest("payKey-1", "order-1", 10000));

        assertThat(res).isNotNull();
        assertThat(res.getPaymentId()).isEqualTo(1L);
        assertThat(res.getWalletBalance()).isEqualTo(50000L);
        verify(tossPaymentsClient).confirm(any(TossConfirmRequest.class));
        verify(pointLedgerService).credit(eq(userId), eq(10000L), eq(PointTransactionType.CHARGE), any(TransactionReference.class));
    }

    @Test
    @DisplayName("결제 승인 실패 - 결제건 없음(NOT_FOUND_PAYMENT)")
    void confirm_fail_notFoundPayment() {
        when(paymentRepository.findByOrderId("order-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirm(2L, new PaymentConfirmRequest("pk", "order-x", 10000)))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.NOT_FOUND_PAYMENT.getMessage());
    }

    @Test
    @DisplayName("결제 승인 실패 - 소유권 불일치(INVALID_USER_ROLE)")
    void confirm_fail_invalidOwner() {
        Payment payment = new Payment(999L, "order-1", 10000);

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirm(2L, new PaymentConfirmRequest("pk", "order-1", 10000)))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.INVALID_USER_ROLE.getMessage());
    }

    @Test
    @DisplayName("결제 승인 실패 - 금액 위변조(INVALID_PAYMENT_AMOUNT) + FAILED 저장")
    void confirm_fail_amountMismatch() {
        Long userId = 2L;
        Payment payment = new Payment(userId, "order-1", 10000);

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> paymentService.confirm(userId, new PaymentConfirmRequest("pk", "order-1", 20000)))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.INVALID_PAYMENT_AMOUNT.getMessage());

        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("결제 승인 실패 - paymentKey 중복(DUPLICATE_PAYMENT_KEY) + FAILED 저장")
    void confirm_fail_duplicatePaymentKey() {
        Long userId = 2L;
        Payment payment = new Payment(userId, "order-1", 10000);

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByPaymentKey("payKey-dup")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> paymentService.confirm(userId, new PaymentConfirmRequest("payKey-dup", "order-1", 10000)))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.DUPLICATE_PAYMENT_KEY.getMessage());

        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("결제 승인 실패 - 토스 confirm 자체 실패(TOSS_CONFIRM_FAILED) + FAILED 저장")
    void confirm_fail_tossConfirmFailed() {
        Long userId = 2L;
        Payment payment = new Payment(userId, "order-1", 10000);

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByPaymentKey("payKey-1")).thenReturn(false);

        when(tossPaymentsClient.confirm(any(TossConfirmRequest.class)))
                .thenThrow(new RuntimeException("toss down"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> paymentService.confirm(userId, new PaymentConfirmRequest("payKey-1", "order-1", 10000)))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.TOSS_CONFIRM_FAILED.getMessage());

        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("결제 승인 실패 - 내부 DB 반영 실패 후 cancel 성공 -> TOSS_CONFIRM_FAILED")
    void confirm_fail_dbAfterConfirm_cancelSuccess() {
        Long userId = 2L;
        Payment payment = new Payment(userId, "order-1", 10000);

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByPaymentKey("payKey-1")).thenReturn(false);

        TossConfirmResponse tossRes = new TossConfirmResponse("payKey-1", "order-1", 10000, "DONE");
        when(tossPaymentsClient.confirm(any(TossConfirmRequest.class))).thenReturn(tossRes);

        // paymentRepository.save(payment)까지는 성공
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // 내부 반영(ledger)이 실패하도록
        when(pointLedgerService.credit(eq(userId), eq(10000L), eq(PointTransactionType.CHARGE), any(TransactionReference.class)))
                .thenThrow(new RuntimeException("db fail"));

        // cancel은 성공
        when(tossPaymentsClient.cancel(eq("payKey-1"), anyString())).thenReturn(null);

        assertThatThrownBy(() -> paymentService.confirm(userId, new PaymentConfirmRequest("payKey-1", "order-1", 10000)))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.TOSS_CONFIRM_FAILED.getMessage());

        verify(tossPaymentsClient).cancel(eq("payKey-1"), anyString());
        verify(paymentRepository, atLeastOnce()).save(payment);
    }

    @Test
    @DisplayName("결제 승인 실패 - 내부 DB 반영 실패 후 cancel도 실패 -> TOSS_CANCEL_FAILED")
    void confirm_fail_dbAfterConfirm_cancelFailed() {
        Long userId = 2L;
        Payment payment = new Payment(userId, "order-1", 10000);

        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByPaymentKey("payKey-1")).thenReturn(false);

        TossConfirmResponse tossRes = new TossConfirmResponse("payKey-1", "order-1", 10000, "DONE");
        when(tossPaymentsClient.confirm(any(TossConfirmRequest.class))).thenReturn(tossRes);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        when(pointLedgerService.credit(eq(userId), eq(10000L), eq(PointTransactionType.CHARGE), any(TransactionReference.class)))
                .thenThrow(new RuntimeException("db fail"));

        // cancel도 실패
        when(tossPaymentsClient.cancel(eq("payKey-1"), anyString()))
                .thenThrow(new RuntimeException("cancel fail"));

        assertThatThrownBy(() -> paymentService.confirm(userId, new PaymentConfirmRequest("payKey-1", "order-1", 10000)))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.TOSS_CANCEL_FAILED.getMessage());

        verify(tossPaymentsClient).cancel(eq("payKey-1"), anyString());
        verify(paymentRepository, atLeastOnce()).save(payment);
    }
}
