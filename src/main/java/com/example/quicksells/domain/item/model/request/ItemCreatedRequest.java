package com.example.quicksells.domain.item.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상품 등록 및 생성")
public class ItemCreatedRequest {

    @NotBlank(message = "상품명은 필수 입니다.")
    @Size(max = 50, message = "상품명은 50자 이내입니다.")
    @Schema(description = "상품 이름")
    private String name;

    @NotNull(message = "상품 희망 가격은 필수입니다.")
    @Min(value = 1, message = "상품 희망 가격은 1원 이상이어야 합니다.")
    @Schema(description = "판매 희망 가격")
    private Long hopePrice;

    @NotBlank(message = "상품 설명은 필수입니다.")
    @Size(max = 500, message = "상품 설명은 500자 이내입니다.")
    @Schema(description = "상품 설명")
    private String description;

}
