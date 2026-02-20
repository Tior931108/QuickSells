package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.model.TransactionReference;
import com.example.quicksells.domain.payment.model.response.WithdrawResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.quicksells.common.enums.UserRole.USER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock private PointLedgerService pointLedgerService;

    @InjectMocks private WithdrawalService withdrawalService;

    @Test
    @DisplayName("출금 실패 - amount null/0/음수면 INVALID_CHARGE_AMOUNT")
    void withdraw_fail_invalidAmount() {
        AuthUser authUser = new AuthUser(2L, "user@test.com", USER, "유저");

        assertThatThrownBy(() -> withdrawalService.withdraw(authUser, null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.INVALID_CHARGE_AMOUNT.getMessage());

        assertThatThrownBy(() -> withdrawalService.withdraw(authUser, 0L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.INVALID_CHARGE_AMOUNT.getMessage());

        assertThatThrownBy(() -> withdrawalService.withdraw(authUser, -1L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.INVALID_CHARGE_AMOUNT.getMessage());

        verify(pointLedgerService, never()).debit(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("출금 성공 - ledger.debit 호출 + 응답 walletBalance 반영")
    void withdraw_success() {
        AuthUser authUser = new AuthUser(2L, "user@test.com", USER, "유저");

        PointWallet wallet = new PointWallet(2L);
        wallet.increaseBalance(50_000L);
        wallet.decreaseBalance(10_000L); // 남은 잔액 40,000

        ArgumentCaptor<TransactionReference> refCaptor = ArgumentCaptor.forClass(TransactionReference.class);

        when(pointLedgerService.debit(eq(2L), eq(10_000L), eq(PointTransactionType.WITHDRAW), any(TransactionReference.class)))
                .thenReturn(wallet);

        WithdrawResponse res = withdrawalService.withdraw(authUser, 10_000L);

        assertThat(res.getAmount()).isEqualTo(10_000L);
        assertThat(res.getWalletBalance()).isEqualTo(40_000L);

        verify(pointLedgerService).debit(eq(2L), eq(10_000L), eq(PointTransactionType.WITHDRAW), refCaptor.capture());
        assertThat(refCaptor.getValue().getPaymentId()).isNull();
        assertThat(refCaptor.getValue().getAuctionId()).isNull();
        assertThat(refCaptor.getValue().getDealId()).isNull();
    }
}
