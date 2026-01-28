package com.example.quicksells.domain.wishlist.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "관심록록 생성")
public class WishListCreateRequest {

    @NotNull(message = "구매자는 필수입니다.")
    @Schema(description = "구매자 ID")
    private Long buyerId;

    @NotNull(message = "상품은 필수입니다")
    @Schema(description = "상품 ID")
    private Long itemId;
}
