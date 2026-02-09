package com.example.quicksells.domain.auth.controller;

import com.example.quicksells.common.util.JwtUtil;
import com.example.quicksells.domain.auth.model.request.AuthLoginRequest;
import com.example.quicksells.domain.auth.model.request.AuthSignupRequest;
import com.example.quicksells.domain.auth.model.response.AuthLoginResponse;
import com.example.quicksells.domain.auth.model.response.AuthSignupResponse;
import com.example.quicksells.domain.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void createUser_success() throws Exception {

        // given
        AuthSignupRequest request = new AuthSignupRequest("test@test.com", "Qwer1234!@", "홍길동", "010-0000-1111", "서울시 관악구", "2001-01-01");
        AuthSignupResponse response = new AuthSignupResponse(1L, "test@test.com", "홍길동", "010-0000-1111", "서울시 관악구", "2001-01-01", "USER", LocalDateTime.now());

        when(authService.createUser(any(AuthSignupRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원 가입 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.phone").value("010-0000-1111"))
                .andExpect(jsonPath("$.data.address").value("서울시 관악구"))
                .andExpect(jsonPath("$.data.birth").value("2001-01-01"));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {

        // given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "Qwer1234!@");
        AuthLoginResponse response = new AuthLoginResponse("token", false);

        when(authService.login(any(AuthLoginRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인 성공하셨습니다."))
                .andExpect(jsonPath("$.data.token").value("token"))
                .andExpect(jsonPath("$.data.passwordResetRequired").value(false));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {

        // given
        String bearerToken = "Bearer token";
        String token = "token";

        when(jwtUtil.substringToken(bearerToken)).thenReturn(token);

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .header(JwtUtil.HEADER_KEY, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃 성공하셨습니다."));

        verify(authService).logout(token);
    }
}