package com.example.quicksells.common.interceptor;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.common.redis.service.TokenBlackListService;
import com.example.quicksells.common.security.JwtAuthenticationToken;
import com.example.quicksells.common.util.JwtUtil;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * WebSocket 메시지 인터셉터
 * - STOMP 연결 시 JWT 토큰 검증
 * - 인증된 사용자 정보 : JwtAuthenticationFilter와 동일한 방식으로 인증 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlackListService tokenBlackListService;

    /**
     * 메시지 전송 전에 실행
     * CONNECT 명령어일 때 JWT 토큰 검증
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // STOMP Header 세션
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // CONNECT 시에만 인증 처리
            String authorizationHeader = extractAuthorizationHeader(accessor);

            // Authorization 헤더 검증
            if (!jwtUtil.hasAuthorizationHeader(authorizationHeader)) {

                throw new CustomException(ExceptionCode.MISSING_TOKEN);
            }

            try {
                // Bearer 제거
                String token = jwtUtil.substringToken(authorizationHeader);

                // 토큰 검증
                if (!jwtUtil.validateToken(token)) {
                    throw new CustomException(ExceptionCode.INVALID_TOKEN);
                }

                // 블랙리스트 체크
                if (tokenBlackListService.isContainToken(token)) {
                    throw new CustomException(ExceptionCode.INVALID_TOKEN);
                }

                // JWT 복호화
                Claims claims = jwtUtil.extractAllClaims(token);

                // AuthUser 활용
                Long userId = Long.valueOf(claims.getSubject());
                String email = claims.get("email", String.class);
                String name = claims.get("name", String.class);
                UserRole userRole = UserRole.of(claims.get("role", String.class));

                AuthUser authUser = new AuthUser(userId, email, userRole, name);

                // JwtAuthenticationToken 생성
                JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(authUser);

                // SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                // STOMP 세션에도 저장
                accessor.setUser(authenticationToken);

                log.info("WebSocket 인증 성공 - UserId: {}, Email: {}, Role: {}", userId, email, userRole);

            } catch (Exception e) {
                log.error("WebSocket 인증 실패: {}", e.getMessage());
                throw new CustomException(ExceptionCode.INVALID_TOKEN);
            }
        }

        return ChannelInterceptor.super.preSend(message, channel);
    }

    /**
     * STOMP 헤더에서 Authorization 헤더 추출
     */
    private String extractAuthorizationHeader(StompHeaderAccessor accessor) {

        // Authorization 헤더 추출
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader != null) {
            return authHeader;
        }

        return null;
    }
}
