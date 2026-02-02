package com.example.quicksells.domain.auth.service;

import com.example.quicksells.common.enums.ExceptionCode;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlackListService tokenBlackListService;
    private final PointWalletRepository pointWalletRepository;

    /**
     * 회원가입 기능
     * @param request 회원가입 요청 정보
     * @return 회원가입 완료된 사용자 정보
     * @throws CustomException 이메일 또는 전화번호가 이미 존재하는 경우
     */
    @Transactional
    public AuthSignupResponse createUser(AuthSignupRequest request) {

        // 이메일, 전화번호 중복 데이터 여부 체크
        boolean exitsEmail = userRepository.existsByEmail(request.getEmail());

        if (exitsEmail) throw new CustomException(ExceptionCode.EXISTS_EMAIL);

        boolean existsPhone = userRepository.existsByPhone(request.getPhone());

        if (existsPhone) throw new CustomException(ExceptionCode.EXISTS_PHONE);

        // 회원가입 요청 저장
        User user = new User(request.getEmail(), passwordEncoder.encode(request.getPassword()), request.getName(), request.getPhone(), request.getAddress(), request.getBirth());

        User savedUser = userRepository.save(user);

        // 유저 생성 시 지갑도 같이 생성, 결제/정산/입찰 로직에서 "지갑 없음" 케이스를 줄이기 위함.
        pointWalletRepository.save(new PointWallet(savedUser.getId()));

        return AuthSignupResponse.from(savedUser);
    }

    /**
     * 로그인 기능
     * @param request 로그인 요청 정보
     * @return 토큰 정보
     * @throws CustomException 이메일 존재 여부 또는 비밀번호 불일치
     */
    @Transactional
    public AuthLoginResponse login(AuthLoginRequest request) {

        // 이메일 체크
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_EMAIL));

        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());

        // 비밀번호 체크
        if (!matches) throw new CustomException(ExceptionCode.NOT_MATCHES_PASSWORD);

        // 토큰 생성
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName(), user.getRole());

        return AuthLoginResponse.from(token);
    }

    @Transactional
    public void logout(String token) {

        long remainingTime = jwtUtil.getRemainingTime(token);

        if (remainingTime > 0) {
            tokenBlackListService.addTokenToBlacklist(token, remainingTime);
        }
    }
}
