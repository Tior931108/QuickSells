package com.example.quicksells.domain.user.controller;

import com.example.quicksells.common.util.JwtUtil;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.user.model.request.UserPasswordUpdateRequest;
import com.example.quicksells.domain.user.model.request.UserProfileUpdateRequest;
import com.example.quicksells.domain.user.model.request.UserRoleUpdateRequest;
import com.example.quicksells.domain.user.model.response.UserGetAllResponse;
import com.example.quicksells.domain.user.model.response.UserGetResponse;
import com.example.quicksells.domain.user.model.response.UserProfileUpdateResponse;
import com.example.quicksells.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "test@test.com", USER, "홍길동");

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
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
                        },

                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyPage_Success() throws Exception {

        // given
        UserGetResponse response = new UserGetResponse(1L, "test@test.com", "홍길동", "010-0000-1111", "서울시 관악구", "20010101", "USER", LocalDateTime.now(), LocalDateTime.now());

        when(userService.getMyPage(any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("내 정보 조회 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.phone").value("010-0000-1111"))
                .andExpect(jsonPath("$.data.address").value("서울시 관악구"))
                .andExpect(jsonPath("$.data.birth").value("20010101"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("내 정보 수정 성공")
    void updateProfile_success() throws Exception {

        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("010-0000-1111", "서울시 관악구");
        UserProfileUpdateResponse response = new UserProfileUpdateResponse(1L, "test@test.com", "홍길동", "010-0000-1111", "서울시 관악구", "20010101", "USER", LocalDateTime.now());

        when(userService.updateProfile(any(), any(UserProfileUpdateRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("내 정보 수정 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.phone").value("010-0000-1111"))
                .andExpect(jsonPath("$.data.address").value("서울시 관악구"))
                .andExpect(jsonPath("$.data.birth").value("20010101"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() throws Exception {

        // given
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPassword123!@#", "newPassword123!@#");

        // when & then
        mockMvc.perform(patch("/api/users/me/passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호 변경 성공하셨습니다."));
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteUser_success() throws Exception {

        // given
        String bearerToken = "Bearer token";
        String token = "token";

        when(jwtUtil.substringToken(bearerToken)).thenReturn(token);

        // when & then
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 탈퇴 성공하셨습니다."));
    }

    @Test
    @DisplayName("전체 사용자 정보 조회 성공(관리자)")
    void getAllUsers_success() throws Exception {

        // given
        UserGetAllResponse user1 = new UserGetAllResponse(1L, "test1@test.com", "홍길동", "010-0000-1111", "서울시 관악구", "20010101", "USER", LocalDateTime.now(), LocalDateTime.now());
        UserGetAllResponse user2 = new UserGetAllResponse(2L, "test2@test.com", "배추도사", "010-0000-2222", "서울시 관악구", "20010102", "USER", LocalDateTime.now(), LocalDateTime.now());
        List<UserGetAllResponse> userList = List.of(user1, user2);

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserGetAllResponse> pageResponse = new PageImpl<>(userList, pageable, userList.size());

        when(userService.getAllUsers(any(Pageable.class))).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("전체 회원 조회 성공하셨습니다."))
                .andExpect(jsonPath("$.data.content[0].email").value("test1@test.com"))
                .andExpect(jsonPath("$.data.content[1].email").value("test2@test.com"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("사용자 권한 변경 성공(관리자)")
    void updateRole_success() throws Exception {

        // given
        Long userId = 1L;
        UserRoleUpdateRequest request = new UserRoleUpdateRequest("ADMIN");

        // when & then
        mockMvc.perform(patch("/api/admin/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("유저 권한 변경 성공하셨습니다."));
    }
}