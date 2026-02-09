package com.example.quicksells.domain.auction.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "경매 검색 필터")
public class AuctionSearchFilterRequest {

    @Min(value = 1, message = "최소 입찰가는 1원 이상이어야 합니다.")
    @Schema(description = "최소 입찰가 검색")
    private Integer minBidPrice; // between 최소 입찰가격
    @Min(value = 1, message = "최대 입찰가는 1원 이상이어야 합니다.")
    @Schema(description = "최대 입찰가 검색")
    private Integer maxBidPrice; // between 최대 입찰가격
    @Schema(description = "감정된 아이템 이름")
    private String appraiseItemName; // 감정된 아이템 이름
}
