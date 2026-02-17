package com.example.quicksells.common.config;

import com.example.quicksells.common.filter.JwtAuthenticationFilter;
import com.example.quicksells.common.security.CustomAccessDeniedHandler;
import com.example.quicksells.common.security.CustomAuthenticationEntryPoint;
import com.example.quicksells.common.security.CustomOAuth2UserService;
import com.example.quicksells.common.security.OAuthSuccessHandler;
import com.example.quicksells.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity // spring security 활성화
@EnableMethodSecurity(securedEnabled = true) // 권한 검사 활성
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 보호 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 사용 안함
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 로그아웃 기능 추가
                .logout(AbstractHttpConfigurer::disable)
                // 인증/ 인가 실패 시 커스텀 응답 반환
                .exceptionHandling(conf ->
                        conf.authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler)
                )
                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // 세션 기반 기능 제거
                .rememberMe(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth.baseUri("/api/auth/login/oauth2"))
                        .userInfoEndpoint(user -> user.userService(customOAuth2UserService))
                        .successHandler(oAuthSuccessHandler())
                )
                // 요청 URL 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/mail","/api/verify-code").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/oauth/google/success/**").permitAll()
                        // 채팅 API 추가 (인증 필요)
                        .requestMatchers("/api/chat/**").authenticated()
                        // WebSocket 엔드포인트 추가
                        .requestMatchers(
                                "/ws-stomp/**",           // WebSocket 연결 엔드포인트
                                "/chat-test.html"         // 실제 사용할 채팅 페이지
                        ).permitAll()
                        // swagger ui 설정 추가
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/payments/config",   // clientKey 제공 (공개키만)
                                "/toss-test.html",
                                "/payment-success.html",
                                "/payment-fail.html"
                        ).permitAll()
                        //log monitoring prometheus
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .anyRequest().authenticated()
                )
                // Spring Security 필터보다 JWT 필터를 먼저 실행
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // 교차 출처 리소스 공유 (CORS: Cross-Origin Resource Sharing)
    // 브라우저가 자신의 출처가 아닌 다른 어떤 출처로부터 자원을 요청하는 것에 대해 허용하도록 서버가 이를 허가해 주는 HTTP 헤더 기반 메커니즘
    // 전역 교차 출처 요청 처리 구성 메서드
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:3000"));                        // 모든 오리진(출처)에서의 요청을 허용
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")); // 허용되는 HTTP 메서드를 지정
        config.setAllowedHeaders(List.of("*"));                                            // 모든 헤더를 허용
        config.setExposedHeaders(List.of("Authorization"));                                // 브라우저에 노출할 응답 헤더를 지정
        config.setAllowCredentials(true);                                                      // 인증된 요청을 허용 (ex: HTTP 인증)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);                                // 모든 경로에 대해 CORS 설정을 적용

        return source;
    }

    @Bean
    public OAuthSuccessHandler oAuthSuccessHandler() { return new OAuthSuccessHandler(jwtUtil); }
}
