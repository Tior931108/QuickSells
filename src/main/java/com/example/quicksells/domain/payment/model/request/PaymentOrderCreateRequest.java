package com.example.quicksells.domain.payment.model.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentOrderCreateRequest {

    /**
     * 충전 금액 (원 단위)
     *
     *  정책
     * - 최소 10,000원 이상
     * - 단위 제한 없음 (예: 12,000 / 37,000 가능)
     */
    @Min(value = 10000, message = "최소 충전 금액은 10,000원입니다.")
    private Integer amount;
}