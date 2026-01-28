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
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/oauth/google/success/**").permitAll()
                        // swagger ui 설정 추가
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // Spring Security 필터보다 JWT 필터를 먼저 실행
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public OAuthSuccessHandler oAuthSuccessHandler() { return new OAuthSuccessHandler(jwtUtil); }
}
