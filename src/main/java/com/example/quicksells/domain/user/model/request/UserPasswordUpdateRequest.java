package com.example.quicksells.domain.user.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "비밀번호 변경 요청")
public class UserPasswordUpdateRequest {

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    @Schema(description = "현재 비밀번호")
    private String currentPassword;

    @NotBlank(message = "변경할 비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-\\[\\]{};':\"\\\\|,.<>/?]).{10,}$",
            message = "비밀번호는 대소문자, 숫자, 특수문자를 포함한 10자 이상이어야 합니다.")
    @Schema(description = "새 비밀번호")
    private String newPassword;
}
