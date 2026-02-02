package com.example.quicksells.domain.user.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "유저 정보 변경")
public class UserProfileUpdateRequest {

    @Pattern(regexp = "^010-\\d{4}-\\d{4}$",
            message = "전화번호는 010-XXXX-XXXX 형식이어야 합니다.")
    @Schema(description = "전화번호 변경")
    private String phone;

    @Pattern(regexp = "^[가-힣a-zA-Z0-9\\s,()-]{5,100}$",
            message = "주소는 5~100자의 한글, 영문, 숫자 및 특수문자(- , ())만 입력 가능합니다.")
    @Schema(description = "주소 변경")
    private String address;

    public boolean isAllFieldEmpty() {return phone == null && address == null;}
}
