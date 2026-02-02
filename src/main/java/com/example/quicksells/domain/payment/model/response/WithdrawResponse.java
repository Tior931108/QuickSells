package com.example.quicksells.domain.payment.model.response;

import com.example.quicksells.domain.payment.entity.PointWallet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 출금 완료 응답 DTO
 */
@Getter
@RequiredArgsConstructor
public class WithdrawResponse {

    private final Long amount;
    private final Long walletBalance;

    public static WithdrawResponse from(Long amount, PointWallet wallet) {
        return new WithdrawResponse(amount, wallet.getAvailableBalance());
    }
}
