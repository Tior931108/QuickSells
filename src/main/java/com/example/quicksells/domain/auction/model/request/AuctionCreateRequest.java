package com.example.quicksells.domain.auction.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "경매 생성")
public class AuctionCreateRequest {

    @NotNull(message = "감정은 필수입니다.")
    @Schema(description = "감정 ID")
    private Long appraiseId;

    @NotNull(message = "경매종료 시간을 입력해주세요.")
    @Min(value = 1, message = "종료시간은 최소 1일부터 선택가능합니다.")
    @Max(value = 3, message = "종료시간은 최대 3일까지 선택가능합니다.")
    @Schema(description = "경매 종료 시간 1~3일")
    private int timeOption;
}
