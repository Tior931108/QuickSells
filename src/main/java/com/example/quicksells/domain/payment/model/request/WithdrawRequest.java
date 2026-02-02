package com.example.quicksells.domain.payment.model.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class WithdrawRequest {

    @Min(value = 1, message = "출금 금액은 1원 이상이어야 합니다.")
    private Long amount;
}