package com.example.quicksells.domain.user.service;

import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.common.redis.service.TokenBlackListService;
import com.example.quicksells.common.util.JwtUtil;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.model.request.UserPasswordUpdateRequest;
import com.example.quicksells.domain.user.model.request.UserProfileUpdateRequest;
import com.example.quicksells.domain.user.model.request.UserRoleUpdateRequest;
import com.example.quicksells.domain.user.model.response.UserGetAllResponse;
import com.example.quicksells.domain.user.model.response.UserGetResponse;
import com.example.quicksells.domain.user.model.response.UserProfileUpdateResponse;
import com.example.quicksells.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.example.quicksells.common.enums.UserRole.USER;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlackListService tokenBlackListService;

    @InjectMocks
    private UserService userService;

    private AuthUser authUser;

    private User user;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "test@test.com", USER, "홍길동");

        user = new User("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("마이페이지 조회 성공")
    void getMyPage_success() {

        // given
        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        // when
        UserGetResponse response = userService.getMyPage(authUser);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getName()).isEqualTo("홍길동");
        assertThat(response.getPhone()).isEqualTo("010-0000-1111");
        assertThat(response.getAddress()).isEqualTo("서울시 관악구");
        assertThat(response.getBirth()).isEqualTo("20010101");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("마이페이지 조회 실패 - 사용자 조회 실패")
    void getMyPage_userNotFound() {

        // given
        when(userRepository.findById(authUser.getId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyPage(authUser))
                .isInstanceOf(CustomException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 변경할 값이 없음")
    void updateProfile_noUpdateFiled() {

        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, null);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(authUser, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("수정할 정보가 없습니다.");
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 전화번호만 변경")
    void updateProfile_updateOnlyPhone() {

        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("010-1111-2222", null);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        // when
        UserProfileUpdateResponse response = userService.updateProfile(authUser, request);

        // then
        assertThat(response.getPhone()).isEqualTo("010-1111-2222");
        assertThat(user.getPhone()).isEqualTo("010-1111-2222");
    }

    @Test
    @DisplayName("전화번호 변경 안 됨 - 변경 전회번호가 기존과 동일")
    void updateProfile_phoneSameValue() {

        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("010-0000-1111", null);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        // when
        userService.updateProfile(authUser, request);

        // then
        assertThat(user.getPhone()).isEqualTo("010-0000-1111");
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 주소만 변경")
    void updateProfile_updateOnlyAddress() {

        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, "서울시 강남구");

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        // when
        UserProfileUpdateResponse response = userService.updateProfile(authUser, request);

        // then
        assertThat(response.getAddress()).isEqualTo("서울시 강남구");
        assertThat(user.getAddress()).isEqualTo("서울시 강남구");
    }

    @Test
    @DisplayName("주소 변경 안 됨 - 변경 주소가 기존과 동일")
    void updateProfile_addressSameValue() {

        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, "서울시 관악구");

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        // when
        userService.updateProfile(authUser, request);

        // then
        assertThat(user.getAddress()).isEqualTo("서울시 관악구");

    }

    @Test
    @DisplayName("내 정보 수정 성공 - 전화번호, 주소 둘 다 변경")
    void updateProfile_updatePhoneAndAddress() {

        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("010-1111-2222", "서울시 강남구");

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        // when
        UserProfileUpdateResponse response = userService.updateProfile(authUser, request);

        // then
        assertThat(response.getPhone()).isEqualTo("010-1111-2222");
        assertThat(response.getAddress()).isEqualTo("서울시 강남구");
        assertThat(user.getPhone()).isEqualTo("010-1111-2222");
        assertThat(user.getAddress()).isEqualTo("서울시 강남구");
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 이미 존재하는 전화번호")
    void updateProfile_phoneAlreadyExists() {

        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("010-1111-2222", null);

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(authUser, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 존재하는 핸드폰 번호입니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void updatePassword_wrongCurrentPassword() {

        // given
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("wrongPassword", "newPassword");

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.updatePassword(authUser, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("현재 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 이전 비밀번호와 동일")
    void updatePassword_sameAsOldPassword() {

        // given
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPassword", "currentPassword");

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updatePassword(authUser, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("이전 비밀번호와 동일합니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() {

        // given
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPassword", "newPassword");

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword", "encodedPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");

        // when
        userService.updatePassword(authUser, request);

        // then
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
        assertThat(user.isPasswordResetRequired()).isFalse();
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void delete_success() {

        // given
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJmbHV4aW5nMTIzQG5hdmVyLmNvbSIsIm5hbWUiOiLstZzsoJXtmIEiLCJyb2xlIjoiVVNFUiIsImlhdCI6MTc3MDA4NjYwMCwiZXhwIjoxNzcwMDkwMjAwfQ.YkgducyoS7S57JQPhBxVHeOfnjJ58QEc6GZ5c2m2HRs";

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        // when
        userService.delete(authUser, token);

        // then
        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("회원 탈퇴시 토큰 블랙리스트에 등록 성공")
    void delete_tokenBlackList_success() {

        // given
        Long remainingTime = 3600L;
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJmbHV4aW5nMTIzQG5hdmVyLmNvbSIsIm5hbWUiOiLstZzsoJXtmIEiLCJyb2xlIjoiVVNFUiIsImlhdCI6MTc3MDA4NjYwMCwiZXhwIjoxNzcwMDkwMjAwfQ.YkgducyoS7S57JQPhBxVHeOfnjJ58QEc6GZ5c2m2HRs";

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));
        when(jwtUtil.getRemainingTime(token)).thenReturn(remainingTime);

        // when
        userService.delete(authUser, token);

        // then
        verify(tokenBlackListService, times(1)).addTokenToBlacklist(token, remainingTime);
    }

    @Test
    @DisplayName("관리자 유저 전체 조회 성공")
    void getAllUsers_success() {

        // given
        User user1 = new User("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        User user2 = new User("test1@test.com", "encodedPassword", "배추도사", "010-0000-2222", "서울시 관악구", "20010102");
        List<User> userList = Arrays.asList(user1, user2);

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = new PageImpl<>(userList, pageable, userList.size());

        when(userRepository.findAllByRole(USER, pageable)).thenReturn(userPage);

        // when
        Page<UserGetAllResponse> responses = userService.getAllUsers(pageable);

        // then
        assertEquals(20, responses.getSize());
        verify(userRepository).findAllByRole(USER, pageable);
    }

    @Test
    @DisplayName("관리자 유저 권한 변경 성공")
    void updateRole_success() {

        // given
        Long userId = 1L;

        UserRoleUpdateRequest request = new UserRoleUpdateRequest("ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        UserProfileUpdateResponse response = userService.updateRole(userId, request);

        // then
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }
}