package com.example.quicksells.domain.auth.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.domain.auth.model.request.AuthMailCodeVerificationRequest;
import com.example.quicksells.domain.auth.model.request.AuthMailRequest;
import com.example.quicksells.domain.auth.model.request.AuthPasswordVerificationRequest;
import com.example.quicksells.domain.auth.service.MailServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MailControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MailServiceImpl mailService;

    @InjectMocks
    private MailController mailController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(mailController)
                .build();
    }

    @Test
    @DisplayName("이메일 인증번호 발송 성공")
    void mailSend_success() throws Exception {

        // given
        AuthMailRequest request = new AuthMailRequest("test@test.com");

        // when $ given
        mockMvc.perform(post("/api/auth/emails/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증번호가 발송되었습니다."));
    }

    @Test
    @DisplayName("이메일 인증번호 검증 성공")
    void verifyCode_success() throws Exception {

        // given
        AuthMailCodeVerificationRequest request = new AuthMailCodeVerificationRequest("test@test.com", 123456);

        // when
        when(mailService.verifyCode(request.getEmail(), request.getCode())).thenReturn(true);

        // then
        mockMvc.perform(post("/api/auth/emails/verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증에 성공하였습니다."));
    }

    @Test
    @DisplayName("이메일 인증번호 검증 실패")
    void verifyCode_fail() throws Exception {

        // given
        AuthMailCodeVerificationRequest request = new AuthMailCodeVerificationRequest("test@test.com", 123456);

        // when
        when(mailService.verifyCode(request.getEmail(), request.getCode())).thenReturn(false);

        // then
        mockMvc.perform(post("/api/auth/emails/verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증번호가 일치하지 않거나 만료되었습니다."));
    }

    @Test
    @DisplayName("임시 비밀번호 재발급 이메일 발송 성공")
    void send_resetPassword_success() throws Exception {

        // given
        AuthMailRequest request = new AuthMailRequest("test@test.com");

        // when
        when(mailService.createTemporaryPassword(request.getEmail())).thenReturn("Qwer1234!!");

        // when $ given
        mockMvc.perform(post("/api/auth/passwords/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("임시 비밀번호가 이메일로 발송되었습니다."));
    }

    @Test
    @DisplayName("임시 비밀번호 검증 성공")
    void verifyTemporaryPassword_success() throws Exception {

        // given
        AuthPasswordVerificationRequest request = new AuthPasswordVerificationRequest("test@test.com", "Qwer1234!!");

        // when
        when(mailService.verifyTemporaryPassword(request.getEmail(), request.getTemporaryPassword())).thenReturn(true);

        // then
        mockMvc.perform(post("/api/auth/passwords/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("임시 비밀번호 인증 성공하였습니다."));
    }

    @Test
    @DisplayName("임시 비밀번호 검증 실패")
    void verifyTemporaryPassword_fail() throws Exception {

        // given
        AuthPasswordVerificationRequest request = new AuthPasswordVerificationRequest("test@test.com", "Qwer1234!!");

        // when
        when(mailService.verifyTemporaryPassword(request.getEmail(), request.getTemporaryPassword())).thenReturn(false);

        // then
        mockMvc.perform(post("/api/auth/passwords/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("임시 비밀빈호가 일치하지 않습니다."));
    }
}