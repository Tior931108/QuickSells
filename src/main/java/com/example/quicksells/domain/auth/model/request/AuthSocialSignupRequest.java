package com.example.quicksells.domain.auth.model.request;

import com.example.quicksells.common.annotation.Adult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class AuthSocialSignupRequest {

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$",
            message = "전화번호는 010-XXXX-XXXX 형식이어야 합니다.")
    private String phone;

    @NotBlank(message = "주소는 필수입니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9\\s,()-]{5,100}$",
            message = "주소는 5~100자의 한글, 영문, 숫자 및 특수문자(- , ())만 입력 가능합니다.")
    private String address;

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(regexp = "^(19|20)\\d{2}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$",
            message = "생년월일은 YYYY-MM-DD 형식이어야 합니다.")
    @Adult
    private String birth;
}


