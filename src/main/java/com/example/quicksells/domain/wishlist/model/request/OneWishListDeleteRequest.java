package com.example.quicksells.domain.wishlist.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관심 목록 삭제")
public class OneWishListDeleteRequest {

    @NotNull(message = "구매자는 필수입니다.")
    @Schema(description = "구매자 ID")
    private Long buyerId;

    @NotNull(message = "삭제할 관심 목록 번호를 지정해주세요.")
    @Min(value = 1, message = "접근이 불가한 관심 목록 번호입니다.")
    @Schema(description = "삭제할 관심 목록 번호")
    private int index; // 0을 포함한 음수의 접근을 막음
}
