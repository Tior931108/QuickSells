package com.example.quicksells.domain.payment.toss;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TossCancelRequest {

    /**
     * 취소 사유
     * - 토스에 전달되는 "취소 이유"
     */
    private String cancelReason;
}