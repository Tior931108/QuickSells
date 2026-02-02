package com.example.quicksells.domain.user.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.common.redis.service.TokenBlackListService;
import com.example.quicksells.common.util.JwtUtil;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.model.request.UserPasswordUpdateRequest;
import com.example.quicksells.domain.user.model.request.UserRoleUpdateRequest;
import com.example.quicksells.domain.user.model.request.UserProfileUpdateRequest;
import com.example.quicksells.domain.user.model.response.UserGetAllResponse;
import com.example.quicksells.domain.user.model.response.UserGetResponse;
import com.example.quicksells.domain.user.model.response.UserProfileUpdateResponse;
import com.example.quicksells.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlackListService tokenBlackListService;

    /**
     * 내 정보 조회 기능
     *
     * @return 내 페이지 정보
     * @throws CustomException 사용자 체크
     */
    @Transactional(readOnly = true)
    public UserGetResponse getMyPage(AuthUser authUser) {

        // 사용자 체크
        User user = findByUserIdOrException(authUser.getId());

        return UserGetResponse.from(user);
    }

    /**
     * 내 정보 수정 기능
     *
     * @param request 내 정보 수정 요청 정보
     * @return 변경된 내 정보
     * @throws CustomException 사용자 체크, 요청에 값이없다면 예외, 전화번호 중복 예외
     */
    @Transactional
    public UserProfileUpdateResponse updateProfile(AuthUser authUser, UserProfileUpdateRequest request) {

        // 사용자 체크
        User user = findByUserIdOrException(authUser.getId());

        // request 값이 비었을 경우 예외
        if (request.isAllFieldEmpty()) throw new CustomException(ExceptionCode.NO_UPDATE_FIELD);

        // 핸드폰 변경 로직
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            // 핸드폰 존재 여부 체크
            validateNewPhone(request.getPhone());
            // 요청 번호로 수정
            user.updatePhone(request.getPhone());
        }

        // 주소 변경 로직
        if (request.getAddress() != null && !request.getAddress().equals(user.getAddress())) {
            user.updateAddress(request.getAddress());
        }

        return UserProfileUpdateResponse.from(user);
    }

    /**
     * 비밀번호 변경 기능
     *
     * @throws CustomException 사용자 체크, 현재 비밀번호 체크, 이전 비밀번호와 동일 여부 체크
     */
    @Transactional
    public void updatePassword(AuthUser authUser,UserPasswordUpdateRequest request) {

        User user = findByUserIdOrException(authUser.getId());

        // 현재 비밀번호 체크
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ExceptionCode.WRONG_PASSWORD);
        }

        // 이전 비밀번호와 동일 여부 체크
        validateNewPassword(request.getNewPassword(), user.getPassword());

        user.updatePassword( passwordEncoder.encode(request.getNewPassword()));
    }

    /**
     * 회원 탈퇴 기능
     *
     * @throws CustomException 사용자 체크, 소프트 딜리트 여부 체크
     */
    @Transactional
    public void delete(AuthUser authUser, String token) {

        // 사용자 및 소프트 딜리트 체크
        User user = findByUserIdOrException(authUser.getId());

        user.delete();

        long remainingTime = jwtUtil.getRemainingTime(token);

        if (remainingTime > 0) {
            tokenBlackListService.addTokenToBlacklist(token, remainingTime);
        }
    }

    /**
     * 전제 유저 정보 조회 기능
     *
     * @return 전체 유저 정보
     */
    @Transactional(readOnly = true)
    public Page<UserGetAllResponse> getAllUsers(Pageable pageable) {

        // 사용자 전체 조회
        return userRepository.findAllByRole(UserRole.USER, pageable)
                .map(UserGetAllResponse::from);
    }

    /**
     * 유저 권한 변경 기능
     *
     * @return 변경된 유저 정보
     */
    @Transactional
    public UserProfileUpdateResponse updateRole(Long userId, UserRoleUpdateRequest request) {

        // 사용자 체크
        User user = findByUserIdOrException(userId);

        // 권한 수정
        user.updateRole(request.getRole());

        return UserProfileUpdateResponse.from(user);
    }

    // 비밀빈호 검증
    private void validateNewPassword(String newPassword, String oldPassword) {

        if (passwordEncoder.matches(newPassword, oldPassword)) {
            throw new CustomException(ExceptionCode.SAME_AS_OLD_PASSWORD);
        }
    }

    // 전화번호 검증
    private void validateNewPhone(String phone) {
        if (userRepository.existsByPhone(phone)) {
            throw new CustomException(ExceptionCode.EXISTS_PHONE);
        }
    }

    // 사용자 체크
    private User findByUserIdOrException(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));
    }


}
