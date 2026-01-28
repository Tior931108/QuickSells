package com.example.quicksells.domain.appraise.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // Jackson이 JSON을 역직렬화할때 기본 생성자가 필요함 : 없으면 논리형 값 삽입시 JSON 파싱 오류 발생
@AllArgsConstructor
@Schema(description = "감정 선택")
public class AppraiseUpdateRequest {

    @NotNull(message = "선택 여부는 필수입니다.")
    @JsonProperty("isSelected")
    @Schema(description = "감정 선택시 true 입력")
    private Boolean isSelected;

    /**
     * Validation 메서드: true만 허용 (V1에서는 경매 미지원)
     * - @AssertTrue는 boolean을 반환하는 is로 시작하는 메서드에 적용
     * - 메서드 이름이 isxxx 형태여야 함
     */
    @AssertTrue(message = "즉시 판매시에 true 선택만 됩니다.")
    private boolean isValidSelection() {
        // isSelected가 null이 아니고 true인 경우만 통과
        return Boolean.TRUE.equals(isSelected);
    }
}
