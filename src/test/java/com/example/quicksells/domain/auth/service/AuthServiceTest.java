package com.example.quicksells.domain.auth.service;

import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.common.redis.service.TokenBlackListService;
import com.example.quicksells.common.util.JwtUtil;
import com.example.quicksells.domain.auth.model.request.AuthLoginRequest;
import com.example.quicksells.domain.auth.model.request.AuthSignupRequest;
import com.example.quicksells.domain.auth.model.response.AuthLoginResponse;
import com.example.quicksells.domain.auth.model.response.AuthSignupResponse;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.repository.PointWalletRepository;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointWalletRepository pointWalletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlackListService tokenBlackListService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("유저 회원가입 성공")
    void createUser_success() {

        // given
        AuthSignupRequest request = new AuthSignupRequest("test@test.com", "password", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        User savedUser = new User(request.getEmail(), "encodedPassword", request.getName(), request.getPhone(), request.getAddress(), request.getBirth());
        ReflectionTestUtils.setField(savedUser, "id", 1L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        AuthSignupResponse response = authService.createUser(request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo(request.getEmail());

        verify(userRepository).save(any(User.class));
        verify(pointWalletRepository).save(any(PointWallet.class));
    }

    @Test
    @DisplayName("유저 회원가입 실패 - 이미 존재하는 이메일")
    void createUser_exists_email() {

        // given
        AuthSignupRequest request = new AuthSignupRequest("test@test.com", "password", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.createUser(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("유저 회원가입 실패 - 이미 존재하는 전화번호")
    void createUser_exists_phone() {

        // given
        AuthSignupRequest request = new AuthSignupRequest("test@test.com", "password", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.createUser(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 존재하는 핸드폰 번호입니다.");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {

        // given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "encodedPassword");

        User user = new User("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName(), user.getRole());

        // when
        AuthLoginResponse response = authService.login(request);

        // then
        assertThat(response.getToken()).isEqualTo(token);
        assertThat(user.isPasswordResetRequired()).isFalse();
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_mismatchPassword() {

        // given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "wrongPassword");
        User user = new User("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("로그아웃 성공 - 토큰 남은 시간만큼 블랙리스트 등록")
    void logout() {

        // given
        Long remainingTime = 3600L;
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJmbHV4aW5nMTIzQG5hdmVyLmNvbSIsIm5hbWUiOiLstZzsoJXtmIEiLCJyb2xlIjoiVVNFUiIsImlhdCI6MTc3MDA4NjYwMCwiZXhwIjoxNzcwMDkwMjAwfQ.YkgducyoS7S57JQPhBxVHeOfnjJ58QEc6GZ5c2m2HRs";

        when(jwtUtil.getRemainingTime(token)).thenReturn(remainingTime);

        // when
        authService.logout(token);

        // then
        verify(tokenBlackListService, times(1)).addTokenToBlacklist(token, remainingTime);
    }

    @Test
    @DisplayName("로그아웃 - 만료된 토큰이면 블랙리스트에 등록 안함")
    void logout_expiredToken_doNothing() {

        // given
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJmbHV4aW5nMTIzQG5hdmVyLmNvbSIsIm5hbWUiOiLstZzsoJXtmIEiLCJyb2xlIjoiVVNFUiIsImlhdCI6MTc3MDA4NjYwMCwiZXhwIjoxNzcwMDkwMjAwfQ.YkgducyoS7S57JQPhBxVHeOfnjJ58QEc6GZ5c2m2HRs";
        long remainingTime = 0L;

        when(jwtUtil.getRemainingTime(token)).thenReturn(remainingTime);

        // when
        authService.logout(token);

        // then
        verify(tokenBlackListService, never()).addTokenToBlacklist(token, remainingTime);
    }
}