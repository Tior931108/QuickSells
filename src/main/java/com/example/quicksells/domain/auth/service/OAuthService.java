package com.example.quicksells.domain.auth.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.UserStatus;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.auth.model.request.AuthSocialSignupRequest;
import com.example.quicksells.domain.auth.model.response.AuthSocialSignupResponse;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthSocialSignupResponse completeSocialSignup(AuthUser authUser, AuthSocialSignupRequest request) {

        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new CustomException(ExceptionCode.ALREADY_COMPLETED_SIGNUP);
        }

        user.completeSignup(request.getPhone(), request.getAddress(), request.getBirth());

        return AuthSocialSignupResponse.from(user);
    }

    @Transactional
    public User createSocialUser(String email, String name) {

        User socialUser = new User(
                email,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                name
        );

        return userRepository.save(socialUser);
    }
}
