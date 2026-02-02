package com.example.quicksells.domain.payment.model.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 토스 결제 clientKey 조회 응답 DTO
 */
@Getter
@RequiredArgsConstructor
public class PaymentConfigResponse {

    private final String clientKey;

    public static PaymentConfigResponse from(String clientKey) {
        return new PaymentConfigResponse(clientKey);
    }
}
