package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.payment.entity.PointTransaction;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.model.response.WithdrawResponse;
import com.example.quicksells.domain.payment.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 출금 서비스
 *
 * 정책:
 * - 실제 은행 송금은 하지 않음
 * - 현재는 내부 포인트 차감 + 거래내역(PointTransaction) 기록만 수행
 */
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final PointWalletService pointWalletService;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional
    public WithdrawResponse withdraw(AuthUser authUser, Long amount) {

        // 1) 금액 검증
        if (amount == null || amount <= 0) {
            throw new CustomException(ExceptionCode.INVALID_CHARGE_AMOUNT);
        }

        // 2) 지갑 조회 (없으면 생성)
        PointWallet wallet = pointWalletService.getOrCreate(authUser.getId());

        // 3) 잔액 범위 내 출금 (부족하면 INSUFFICIENT_BALANCE 예외)
        wallet.decreaseBalance(amount);

        // 4) 출금 거래내역 기록 (paymentId/auctionId/dealId 등은 null)
        pointTransactionRepository.save(new PointTransaction(
                authUser.getId(),
                PointTransactionType.WITHDRAW,
                amount,
                null,
                null
        ));

        // 5) 응답
        return WithdrawResponse.from(amount, wallet);
    }
}
