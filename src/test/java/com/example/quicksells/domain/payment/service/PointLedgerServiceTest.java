package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.domain.payment.entity.PointTransaction;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.model.TransactionReference;
import com.example.quicksells.domain.payment.repository.PointTransactionRepository;
import com.example.quicksells.domain.payment.repository.PointWalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointLedgerServiceTest {

    @Mock private PointWalletRepository pointWalletRepository;
    @Mock private PointTransactionRepository pointTransactionRepository;

    @InjectMocks private PointLedgerService pointLedgerService;

    @Test
    @DisplayName("getOrCreate - 지갑 존재하면 그대로 반환(save 호출 없음)")
    void getOrCreate_existing() {
        PointWallet wallet = new PointWallet(1L);
        when(pointWalletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        PointWallet result = pointLedgerService.getOrCreate(1L);

        assertThat(result).isSameAs(wallet);
        verify(pointWalletRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreate - 지갑 없으면 생성 후 save")
    void getOrCreate_create() {
        when(pointWalletRepository.findById(1L)).thenReturn(Optional.empty());
        when(pointWalletRepository.save(any(PointWallet.class))).thenAnswer(inv -> inv.getArgument(0));

        PointWallet result = pointLedgerService.getOrCreate(1L);

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getAvailableBalance()).isEqualTo(0L);
        verify(pointWalletRepository).save(any(PointWallet.class));
    }

    @Test
    @DisplayName("credit - 잔액 증가 + 거래내역 저장")
    void credit_increase_and_saveTransaction() {
        PointWallet wallet = new PointWallet(1L);
        when(pointWalletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PointWallet result = pointLedgerService.credit(
                1L, 10_000L, PointTransactionType.CHARGE, TransactionReference.ofPayment(5L)
        );

        assertThat(result.getAvailableBalance()).isEqualTo(10_000L);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(captor.capture());

        PointTransaction tx = captor.getValue();
        assertThat(tx.getUserId()).isEqualTo(1L);
        assertThat(tx.getTransactionType()).isEqualTo(PointTransactionType.CHARGE);
        assertThat(tx.getAmount()).isEqualTo(10_000L);
        assertThat(tx.getPaymentId()).isEqualTo(5L);
        assertThat(tx.getAuctionId()).isNull();
        assertThat(tx.getDealId()).isNull();
    }

    @Test
    @DisplayName("debit - 잔액 감소 + 거래내역 저장")
    void debit_decrease_and_saveTransaction() {
        PointWallet wallet = new PointWallet(1L);
        // 미리 잔액 채워두기
        wallet.increaseBalance(20_000L);

        when(pointWalletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PointWallet result = pointLedgerService.debit(
                1L, 5_000L, PointTransactionType.WITHDRAW, TransactionReference.none()
        );

        assertThat(result.getAvailableBalance()).isEqualTo(15_000L);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionType()).isEqualTo(PointTransactionType.WITHDRAW);
    }

    @Test
    @DisplayName("transfer - from 감소/to 증가 + 거래내역 2건 저장")
    void transfer_moves_money_and_saves_2_transactions() {
        PointWallet from = new PointWallet(1L);
        from.increaseBalance(30_000L);

        PointWallet to = new PointWallet(2L);

        when(pointWalletRepository.findById(1L)).thenReturn(Optional.of(from));
        when(pointWalletRepository.findById(2L)).thenReturn(Optional.of(to));
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        pointLedgerService.transfer(
                1L, 2L, 10_000L,
                PointTransactionType.AUCTION_WIN_DEDUCT,
                PointTransactionType.AUCTION_SELLER_CREDIT,
                TransactionReference.ofAuction(7L)
        );

        assertThat(from.getAvailableBalance()).isEqualTo(20_000L);
        assertThat(to.getAvailableBalance()).isEqualTo(10_000L);

        verify(pointTransactionRepository, times(2)).save(any(PointTransaction.class));
    }
}
