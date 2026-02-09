package com.example.quicksells.domain.payment.model.response;

import com.example.quicksells.domain.payment.entity.PointWallet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AdminPointGrantResponse {

    private final Long userId;
    private final Long amount;
    private final Long walletBalance;

    public static AdminPointGrantResponse from(Long userId, Long amount, PointWallet wallet) {
        return new AdminPointGrantResponse(userId, amount, wallet.getAvailableBalance());
    }
}
