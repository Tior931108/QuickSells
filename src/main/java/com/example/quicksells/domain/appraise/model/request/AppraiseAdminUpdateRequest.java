package com.example.quicksells.domain.appraise.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "감정가 수정")
public class AppraiseAdminUpdateRequest {

    @NotNull(message = "감정가는 필수입니다.")
    @Min(value = 1, message = "감정가는 1원 이상이어야 합니다.")
    @Schema(description = "감정가 수정")
    private Integer bidPrice;
}
