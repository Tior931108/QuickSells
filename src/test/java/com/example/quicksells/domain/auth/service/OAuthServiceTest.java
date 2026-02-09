package com.example.quicksells.domain.auth.service;

import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.auth.model.request.AuthSocialSignupRequest;
import com.example.quicksells.domain.auth.model.response.AuthSocialSignupResponse;
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

import static com.example.quicksells.common.enums.UserRole.USER;
import static com.example.quicksells.common.enums.UserStatus.ACTIVE;
import static com.example.quicksells.common.enums.UserStatus.PENDING;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointWalletRepository pointWalletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OAuthService oAuthService;

    @Test
    @DisplayName("소셜 로그인 가입 성공")
    void createSocialUser_success() {

        // given
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        User socialUser = new User("test@test.com", "encodedPassword", "홍길동", "providerId");
        ReflectionTestUtils.setField(socialUser, "id", 1L);

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(socialUser);

        // when
        User response = oAuthService.createSocialUser("test@test.com", "홍길동", "providerId");

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getName()).isEqualTo("홍길동");
        assertThat(response.getProviderId()).isEqualTo("providerId");

        verify(userRepository).save(any(User.class));
        verify(pointWalletRepository).save(any(PointWallet.class));
    }

    @Test
    @DisplayName("소셜 로그인 가입 실패 - 이미 존재하는 이메일")
    void createSocialUser_fail() {

        // given
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> oAuthService.createSocialUser("test@test.com", "홍길동", "providerId"))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("소셜 로그인 후 추가 정보 입력 성공")
    void completeSocialSignup_success() {

        // given
        AuthUser authUser = new AuthUser(1L, "test@test.com", USER, "홍길동");

        AuthSocialSignupRequest request  = new AuthSocialSignupRequest("010-0000-1111", "서울시 관악구", "20010101");

        User user = new User ("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user,"status",PENDING);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        AuthSocialSignupResponse response = oAuthService.completeSocialSignup(authUser, request);

        assertThat(response.getPhone()).isEqualTo("010-0000-1111");
        assertThat(response.getAddress()).isEqualTo("서울시 관악구");
        assertThat(response.getBirth()).isEqualTo("20010101");
    }

    @Test
    @DisplayName("소셜 로그인 후 추가 정보 입력 실패 - 이미 가입된 사용자")
    void completeSocialSignup_fail() {

        // given
        AuthUser authUser = new AuthUser(1L, "test@test.com", USER, "홍길동");

        AuthSocialSignupRequest request  = new AuthSocialSignupRequest("010-0000-1111", "서울시 관악구", "20010101");

        User user = new User ("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user,"status",ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> oAuthService.completeSocialSignup(authUser, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 회원가입이 완료된 사용자입니다.");
    }
}