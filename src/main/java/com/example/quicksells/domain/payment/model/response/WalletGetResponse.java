package com.example.quicksells.domain.payment.model.response;

import com.example.quicksells.domain.payment.entity.PointWallet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WalletGetResponse {

    private final Long userId;
    private final Long availableBalance;

    public static WalletGetResponse from(PointWallet wallet) {
        return new WalletGetResponse(wallet.getUserId(), wallet.getAvailableBalance());
    }
}
