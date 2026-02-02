package com.example.quicksells.domain.user.model.response;

import com.example.quicksells.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class UserProfileUpdateResponse {

    private final Long id;
    private final String email;
    private final String name;
    private final String phone;
    private final String address;
    private final String birth;
    private final String role;
    private final LocalDateTime updatedAt;


    public static UserProfileUpdateResponse from(User user){
        return new UserProfileUpdateResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getAddress(),
                user.getBirth(),
                user.getRole().name(),
                user.getUpdatedAt());
    }
}
