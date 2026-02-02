package com.example.quicksells.domain.payment.model.response;

import com.example.quicksells.domain.payment.entity.Payment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentOrderCreateResponse {

    /**
     * 서버가 발급한 주문번호
     * - 결제창 오픈/승인(confirm) 시 필수
     */
    private final String orderId;

    /**
     * 주문 금액
     * - confirm 위/변조 방지용으로 서버 저장값과 비교
     */
    private final Integer amount;

    public static PaymentOrderCreateResponse from(Payment payment) {
        return new PaymentOrderCreateResponse(payment.getOrderId(), payment.getAmount());
    }
}