package com.example.quicksells.common.security;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.service.OAuthService;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuthService oAuthService;

    /**
     * loadUser: 리소스 서버(Google)로부터 액세스 토큰을 받은 후 실행
     * 이 메서드에서 사용자 정보를 추출하고 DB 저장 및 권한 부여를 처리
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {

        // 리소스 서버에 사용자 정보 요청 후 리소스 객체(OAuth2User) 받아오기
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        // 어떤 소셜 서비스인지 구분하기 위한 ID 추출 (공급자: Google)
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();

        // 구글 로그인만 지원하므로 서비스 제공자 검증
        if (!"google".equals(registrationId)) {
            throw new CustomException(ExceptionCode.OAUTH_PROVIDER_NOT_SUPPORTED);
        }

        // 구글의 고유 PK인 'sub' 추출
        String providerId = oAuth2User.getAttribute("sub");

        // 리소스 서버에서 넘어온 전체 속성 확인
        Map<String, Object> attributes = oAuth2User.getAttributes();
        // 소셜 사용자 정보에서 데이터(이메일, 이름) 추출
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");


        // 기존 회원이면 조회하고, 신규 회원이면 DB에 저장
        User user = userRepository.findByProviderId(providerId)
                .orElseGet(() -> oAuthService.createSocialUser(email, name, providerId));

        // Spring Security 내부 세션에 저장할 인증 객체(AuthDetails) 생성 및 반환
        return new AuthDetails(user, attributes, Collections.singleton(new SimpleGrantedAuthority(user.getRole().getUserRole())));
    }
}
