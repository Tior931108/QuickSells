package com.example.quicksells.domain.auth.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthPasswordVerificationRequest {

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    @Schema(description = "이메일")
    private String email;

    @NotBlank(message = "임시 비밀번호를 입력해주세요.")
    @Schema(description = "발급받은 임시 비밀번호")
    private String temporaryPassword;
}
