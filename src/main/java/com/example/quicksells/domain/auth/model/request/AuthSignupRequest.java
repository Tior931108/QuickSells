package com.example.quicksells.domain.auth.model.request;

import com.example.quicksells.common.annotation.Adult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "회원가입")
public class AuthSignupRequest {

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    @Schema(description = "이메일")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-\\[\\]{};':\"\\\\|,.<>/?]).{10,}$",
            message = "비밀번호는 대소문자, 숫자, 특수문자를 포함한 10자 이상이어야 합니다.")
    @Schema(description = "비밀번호")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Pattern(regexp = "^[가-힣]{2,20}$",
            message = "이름은 2~20자의 한글만 입력 가능합니다.")
    @Schema(description = "이름(실명)")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$",
            message = "전화번호는 010-XXXX-XXXX 형식이어야 합니다.")
    @Schema(description = "핸드폰 전화번호")
    private String phone;

    @NotBlank(message = "주소는 필수입니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9\\s,()-]{5,100}$",
            message = "주소는 5~100자의 한글, 영문, 숫자 및 특수문자(- , ())만 입력 가능합니다.")
    @Schema(description = "주소")
    private String address;

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(regexp = "^(19|20)\\d{2}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$",
            message = "생년월일은 YYYY-MM-DD 형식이어야 합니다.")
    @Adult
    @Schema(description = "생년월일(YYYY-MM-DD, 만19세 이상 성인만)")
    private String birth;
}
