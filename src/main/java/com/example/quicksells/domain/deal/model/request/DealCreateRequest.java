package com.example.quicksells.domain.deal.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "거래 생성")
public class DealCreateRequest {

    @NotNull(message = "감정 ID는 필수입니다.")
    @Schema(description = "감정 ID")
    private Long appraiseId;

    @NotNull(message = "거래 가격은 필수입니다.")
    @Schema(description = "거래 가격")
    private Integer dealPrice;
}
