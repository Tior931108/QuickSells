package com.example.quicksells.common.security;

import com.example.quicksells.common.enums.UserStatus;
import com.example.quicksells.common.util.JwtUtil;
import com.example.quicksells.domain.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class OAuthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    // 프론트엔드 환경의 리다이렉트 주소 yml 설정
    @Value("${frontend.redirect-url}")
    private String frontRedirectUrl;

    /**
     * OAuth2 인증이 최종 성공했을 때 실행되는 메서드
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        // CustomOAuth2UserService에서 생성한 인증 객체(AuthDetails)를 가져옴
        AuthDetails authDetails = (AuthDetails) authentication.getPrincipal();

        // 인증 객체 내부의 User 엔티티 정보를 추출
        User user = authDetails.getUser();

        // 소셜 로그인 후 정보 입력을 안했을 때
        if (user.getStatus() == UserStatus.PENDING) {

            // 프론트에서 서버 OAuthController {/oauth/google/signup} url 호출
            String targetUrl = UriComponentsBuilder.fromUriString(frontRedirectUrl + "/oauth/additional-info")
                    .queryParam("userId", user.getId())
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // Access Token 생성
        String accessToken = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );

        // 로컬 개발 환경인지, 프론트 서버 환경인지에 따라 목적지 설정 (리다이렉트 URL 결정)
        // 프론트에서 서버 OAuthController {/oauth/google/success} url 호출
        String targetUrl = UriComponentsBuilder.fromUriString(frontRedirectUrl + "/oauth/callback")
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        log.info("OAuth 로그인 성공! 리다이렉트 주소: {}", targetUrl);
        // 설정된 URL로 브라우저를 리다이렉트 시킴
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
