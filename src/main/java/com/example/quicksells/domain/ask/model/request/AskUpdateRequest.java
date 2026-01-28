package com.example.quicksells.domain.ask.model.request;

import com.example.quicksells.common.annotation.EnumValidator;
import com.example.quicksells.common.enums.AskType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "문의 수정")
public class AskUpdateRequest {

    @NotBlank(message = "문의 유형은 필수입니다.")
    @EnumValidator(
            targetEnum = AskType.class,
            message = "문의 유형은 ITEM, APPRAISE, AUCTION, PAY, USER, ETC 중에 하나입니다."
    )
    @Schema(description = "문의 유형 선택")
    private String askType;

    @Size(max = 50, message = "문의 제목은 50자 이내여야 합니다.")
    @Schema(description = "문의 제목 수정")
    private String title;

    @Size(max = 500, message = "문의 내용은 500자 이내여야 합니다.")
    @Schema(description = "문의 내용 수정")
    private String content;

    public AskType getAskType() {
        return AskType.valueOf(askType.toUpperCase());
    }
}
