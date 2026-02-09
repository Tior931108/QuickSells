package com.example.quicksells.domain.auth.service;

import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock
    JavaMailSender javaMailSender;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MimeMessage mimeMessage;

    @InjectMocks
    MailServiceImpl mailService;

    @Test
    @DisplayName("인증 코드 메일 전송 성공")
    void sendCodeMail_success() {

        // given
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        mailService.sendCodeMail("test@test.com");

        // then
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("인증 코드 메일 전송 실패")
    void sendCodeMail_fail() {

        // given
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        try (var mocked = mockConstruction(
                MimeMessageHelper.class,
                (mock, context) ->
                        doThrow(MessagingException.class)
                                .when(mock)
                                .setText(anyString(), eq(true)))) {

            // then
            assertThatThrownBy(() -> mailService.sendCodeMail("test@test.com"))
                    .isInstanceOf(RuntimeException.class);
        }
    }


    @Test
    @DisplayName("인증 코드 검증 성공")
    void verifyCode_success() {

        // given
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        mailService.sendCodeMail("test@test.com");
        Integer code = getVerificationCode("test@test.com");

        // then
        assertTrue(mailService.verifyCode("test@test.com", code));
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 코드 불일치")
    void verifyCode_fail_wrongCode() {

        // given
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        mailService.sendCodeMail("test@test.com");

        // then
        assertFalse(mailService.verifyCode("test@test.com", 111111));
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 코드 없음")
    void verifyCode_fail_noCode() {

        // when & then
        assertFalse(mailService.verifyCode("test@test.com", 123456));
    }

    @Test
    @DisplayName("임시 비밀번호 발급 성공")
    void createTemporaryPassword_success() {

        // given
        User user = new User("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedTemporaryPassword");

        // when
        String tempPassword = mailService.createTemporaryPassword("test@test.com");

        // then
        assertNotNull(tempPassword);
        assertThat(user.getPassword()).isEqualTo("encodedTemporaryPassword");
        assertTrue(user.isPasswordResetRequired());
    }

    @Test
    @DisplayName("임시 비밀번호 발급 실패 - 사용자 없음")
    void createTemporaryPassword_fail_userNotFound() {

        // given
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> mailService.createTemporaryPassword("test@test.com"))
                .isInstanceOf(CustomException.class)
                .hasMessage("이메일을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("임시 비밀번호 메일 전송 성공")
    void sendTemporaryPasswordMail_success() {

        // given
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        mailService.sendTemporaryPasswordMail("test@test.com", "temp1234");

        // then
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("임시 비밀번호 검증 성공")
    void verifyTemporaryPassword_success() {

        // given
        User user = mock(User.class);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(user.getPassword()).thenReturn("encodedPassword");
        when(passwordEncoder.matches("temporaryPassword", "encodedPassword")).thenReturn(true);

        // when & then
        assertTrue(mailService.verifyTemporaryPassword("test@test.com", "temporaryPassword"));
    }

    @Test
    @DisplayName("임시 비밀번호 검증 실패 - 사용자 없음")
    void verifyTemporaryPassword_fail_userNotFound() {

        // given
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> mailService.verifyTemporaryPassword("test@test.com", "temp"))
                .isInstanceOf(CustomException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    private Integer getVerificationCode(String email) {
        try {
            Field field = MailServiceImpl.class
                    .getDeclaredField("verificationCodes");
            field.setAccessible(true);
            Map<String, Integer> map =
                    (Map<String, Integer>) field.get(mailService);
            return map.get(email);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
