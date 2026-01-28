package com.example.quicksells.domain.user.model.request;

import com.example.quicksells.common.annotation.EnumValidator;
import com.example.quicksells.common.enums.AskType;
import com.example.quicksells.common.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "유저 권한 변경")
public class UserRoleUpdateRequest {

    @NotBlank
    @EnumValidator(
            targetEnum = UserRole.class,
            message = "유저 권한은 USER, ADMIN 중에 하나입니다."
    )
    @Schema(description = "유저 권한")
    private String role;
}
