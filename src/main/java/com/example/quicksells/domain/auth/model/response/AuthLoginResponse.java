package com.example.quicksells.domain.auth.model.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthLoginResponse {

    private final String token;
    private final boolean passwordResetRequired;

    public static AuthLoginResponse from(String token, boolean passwordResetRequired) {
        return new AuthLoginResponse(
                token,
                passwordResetRequired);
    }
}
