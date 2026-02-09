package com.example.quicksells.domain.payment.model.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class AdminPointGrantRequest {

    @Min(value = 1, message = "지급 금액은 1원 이상이어야 합니다.")
    private Long amount;
}
