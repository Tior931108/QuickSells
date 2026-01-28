package com.example.quicksells.common.enums;

import com.example.quicksells.common.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    USER(Authority.USER),
    ADMIN(Authority.ADMIN),
    APPRAISER(Authority.APPRAISER);


    // ex) "ROLE_USER"
    private final String userRole;

    // 문자열을 UserRole enum 으로 변환 (JWT에서 꺼낸 role 값)
    public static UserRole of(String userRole) {
        return Arrays.stream(UserRole.values())
                .filter(r -> r.name().equalsIgnoreCase(userRole))
                .findFirst()
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_USER_ROLE));
    }

    // Spring Security 규칙 ROLE_ 접두사
    public static class Authority {
        public static final String USER = "ROLE_USER";
        public static final String ADMIN = "ROLE_ADMIN";
        public static final String APPRAISER = "ROLE_APPRAISER";
    }
}
