package com.example.quicksells.domain.item.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "상품 수정")
public class ItemUpdateRequest {

    @NotBlank(message = "상품명은 필수 입니다.")
    @Size(max = 255, message = "상품명은 255자 이내입니다.")
    @Schema(description = "상품 이름 수정")
    private String name;

    @NotNull(message = "상품 희망 가격은 필수입니다.")
    @Min(value = 1, message = "상품 희망 가격은 1원 이상이어야 합니다.")
    @Schema(description = "희망 가격 수정")
    private Long hopePrice;

    @NotBlank(message = "상품 설명은 필수입니다.")
    @Size(max = 500, message = "상품 설명은 500자 이내입니다.")
    @Schema(description = "상품 설명 수정")
    private String description;

    @Schema(description = "이미지 변경")
    private Boolean image;
}
