package com.example.quicksells.domain.auth.controller;

import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.auth.model.request.AuthSocialSignupRequest;
import com.example.quicksells.domain.auth.model.response.AuthSocialSignupResponse;
import com.example.quicksells.domain.auth.service.OAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;

import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OAuthService oAuthService;

    @InjectMocks
    private OAuthController oAuthController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "test@test.com", USER, "홍길동");

        mockMvc = MockMvcBuilders.standaloneSetup(oAuthController)
                .setCustomArgumentResolvers(
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().equals(AuthUser.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return authUser;
                            }
                        }
                )
                .build();
    }

    @Test
    @DisplayName("구글 로그인 성공")
    void oauthGoogle_success() throws Exception {

        // given
        String accessToken = "accessToken";

        // when & then
        mockMvc.perform(get("/oauth/google/success")
                        .param("accessToken", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("구글 로그인에 성공하셨습니다."))
                .andExpect(jsonPath("$.data").value(accessToken));
    }

    @Test
    @DisplayName("구글 로그인 후 추가 정보 입력 성공")
    void socialSignup_success() throws Exception {

        // given
        AuthSocialSignupRequest request = new AuthSocialSignupRequest("010-0000-1111", "서울시 관악구", "2001-01-01");
        AuthSocialSignupResponse response = new AuthSocialSignupResponse(1L, "test@test.com", "홍길동", "010-0000-1111", "서울시 관악구", "2001-01-01", "USER", LocalDateTime.now());

        when(oAuthService.completeSocialSignup(any(),any(AuthSocialSignupRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/oauth/google/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("소셜 회원가입을 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.phone").value("010-0000-1111"))
                .andExpect(jsonPath("$.data.address").value("서울시 관악구"))
                .andExpect(jsonPath("$.data.birth").value("2001-01-01"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }
}