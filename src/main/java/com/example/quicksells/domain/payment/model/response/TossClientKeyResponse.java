package com.example.quicksells.domain.payment.model.response;

import lombok.Getter;

/**
 * 백엔드 단독 테스트를 위한 토스 clientKey 제공 DTO
 *
 *  주의
 * - clientKey는 "공개키" 성격이라 노출 가능
 * - secretKey는 절대 내려주면 안 됨!!!!!
 */
@Getter
public class TossClientKeyResponse {
    private final String clientKey;

    public TossClientKeyResponse(String clientKey) {
        this.clientKey = clientKey;
    }
}
