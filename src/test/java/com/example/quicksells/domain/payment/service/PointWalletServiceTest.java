package com.example.quicksells.domain.payment.service;

import com.example.quicksells.domain.payment.entity.PointTransaction;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.model.response.AdminPointGrantResponse;
import com.example.quicksells.domain.payment.model.response.PointTransactionGetResponse;
import com.example.quicksells.domain.payment.model.response.WalletGetResponse;
import com.example.quicksells.domain.payment.repository.PointTransactionRepository;
import com.example.quicksells.domain.payment.repository.PointWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointWalletServiceTest {

    @Mock private PointWalletRepository pointWalletRepository;
    @Mock private PointTransactionRepository pointTransactionRepository;
    @Mock private PointLedgerService pointLedgerService;

    @InjectMocks private PointWalletService pointWalletService;

    @BeforeEach
    void setUp() {
        // nothing
    }

    @Test
    @DisplayName("getMyWalletResponse - 지갑 조회 응답 매핑(없으면 생성)")
    void getMyWalletResponse_mapsWallet() {
        // given: 지갑이 없어서 생성 흐름 타게
        when(pointWalletRepository.findById(1L)).thenReturn(Optional.empty());
        when(pointWalletRepository.save(any(PointWallet.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        WalletGetResponse res = pointWalletService.getMyWalletResponse(1L);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getUserId()).isEqualTo(1L);
        verify(pointWalletRepository).save(any(PointWallet.class));
    }

    @Test
    @DisplayName("getMyTransactionsResponse - 내 거래내역 조회: repository 호출 + map(from) 적용")
    void getMyTransactionsResponse_callsRepo_andMaps() {
        // given
        long userId = 1L;
        int page = 0, size = 10;

        PointTransaction tx = mock(PointTransaction.class);
        Page<PointTransaction> txPage = new PageImpl<>(
                List.of(tx),
                PageRequest.of(page, size),
                1
        );

        when(pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(txPage);

        // when
        Page<PointTransactionGetResponse> res = pointWalletService.getMyTransactionsResponse(userId, page, size);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getTotalElements()).isEqualTo(1);

        verify(pointTransactionRepository)
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Test
    @DisplayName("grantPointResponse - 관리자 포인트 지급 응답 매핑(ledger credit 호출됨)")
    void grantPointResponse_mapsAdminResponse() {
        // given
        long targetUserId = 2L;
        long amount = 10_000L;

        PointWallet wallet = new PointWallet(targetUserId);
        wallet.increaseBalance(amount);

        // PointWalletService.grantPoint 내부에서 pointLedgerService.credit(...)을 호출하는 구조였음
        when(pointLedgerService.credit(eq(targetUserId), eq(amount), any(), any()))
                .thenReturn(wallet);

        // when
        AdminPointGrantResponse res = pointWalletService.grantPointResponse(targetUserId, amount);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getUserId()).isEqualTo(targetUserId);
        assertThat(res.getAmount()).isEqualTo(amount);
        assertThat(res.getWalletBalance()).isEqualTo(wallet.getAvailableBalance());

        verify(pointLedgerService).credit(eq(targetUserId), eq(amount), any(), any());
    }

    @Test
    @DisplayName("getUserWalletResponse - 관리자: 특정 유저 지갑 조회 응답 매핑")
    void getUserWalletResponse_mapsWallet() {
        // given: 기존 지갑 존재
        PointWallet wallet = new PointWallet(5L);
        wallet.increaseBalance(12345L);

        when(pointWalletRepository.findById(5L)).thenReturn(Optional.of(wallet));

        // when
        WalletGetResponse res = pointWalletService.getUserWalletResponse(5L);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getUserId()).isEqualTo(5L);
        assertThat(res.getAvailableBalance()).isEqualTo(12345L);

        verify(pointWalletRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserTransactionsResponse - 관리자: 특정 유저 거래내역 조회 repository 호출 + map(from)")
    void getUserTransactionsResponse_callsRepo_andMaps() {
        // given
        long userId = 5L;
        int page = 1, size = 1;

        PointTransaction tx = mock(PointTransaction.class);
        Page<PointTransaction> txPage = new PageImpl<>(
                List.of(tx),
                PageRequest.of(page, size),
                1
        );

        when(pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(txPage);

        // when
        Page<PointTransactionGetResponse> res = pointWalletService.getUserTransactionsResponse(userId, page, size);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getTotalElements()).isEqualTo(2);

        verify(pointTransactionRepository)
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }
}
