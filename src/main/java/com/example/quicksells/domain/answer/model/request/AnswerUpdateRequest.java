package com.example.quicksells.domain.answer.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "문의 답변 수정")
public class AnswerUpdateRequest {

    @NotBlank(message = "수정할 답변 제목을 입력해 주세요.")
    @Schema(description = "문의 답변 제목")
    private String title;

    @NotBlank(message = "수정할 답변 내용을 입력해 주세요.")
    @Schema(description = "문의 답변 내용")
    private String content;
}
