package com.example.quicksells.domain.answer.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "문의 답변 생성")
public class AnswerCreateRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "문의 답변 제목")
    private String title;

    @NotBlank
    @Size(max = 500)
    @Schema(description = "문의 답변 내용")
    private String content;
}
