package com.example.quicksells.domain.auth.model.response;

import com.example.quicksells.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class AuthSocialSignupResponse {

    private final Long id;
    private final String email;
    private final String name;
    private final String phone;
    private final String address;
    private final String birth;
    private final String role;
    private final LocalDateTime createdAt;


    public static AuthSocialSignupResponse from(User user) {
        return new AuthSocialSignupResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getAddress(),
                user.getBirth(),
                user.getRole().name(),
                user.getCreatedAt());
    }
}
