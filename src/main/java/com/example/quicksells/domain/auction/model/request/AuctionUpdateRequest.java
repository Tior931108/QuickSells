package com.example.quicksells.domain.auction.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "경매 입찰")
public class AuctionUpdateRequest {

    @NotNull(message = "구매자는 필수입니다.")
    @Schema(description = "구매자 ID")
    private Long buyerId;

    @NotNull(message = "입찰 가격을 입력해주세요.")
    @Schema(description = "입찰 가격")
    private Integer bidPrice;
}
