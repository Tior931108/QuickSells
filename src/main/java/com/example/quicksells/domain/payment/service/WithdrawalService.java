package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.model.TransactionReference;
import com.example.quicksells.domain.payment.model.response.WithdrawResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 출금 서비스
 * - 내부 포인트 차감 + 거래내역 기록만 수행
 */
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final PointLedgerService pointLedgerService;

    @Transactional
    public WithdrawResponse withdraw(AuthUser authUser, Long amount) {

        if (amount == null || amount <= 0) {
            throw new CustomException(ExceptionCode.INVALID_CHARGE_AMOUNT);
        }

        PointWallet wallet = pointLedgerService.debit(
                authUser.getId(),
                amount,
                PointTransactionType.WITHDRAW,
                TransactionReference.none()
        );

        return WithdrawResponse.from(amount, wallet);
    }
}
