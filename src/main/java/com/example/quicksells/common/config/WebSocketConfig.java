package com.example.quicksells.common.config;

import com.example.quicksells.common.interceptor.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 설정
 *
 * 1. /ws-stomp: WebSocket 연결 엔드포인트 (클라이언트가 최초 연결)
 * 2. /app: 클라이언트가 메시지를 보낼 때 사용하는 prefix
 * 3. /topic: 브로드캐스트 (1:N, 여러 명이 구독) - 그룹 채팅은 후순위
 * 4. /queue: 개인 메시지 (1:1, 특정 사용자만)
 */
@Configuration
@EnableWebSocketMessageBroker // websocket 메시지 브로커 활성화
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * STOMP 엔드포인트 등록
     * 클라이언트가 WebSocket에 연결할 때 사용하는 URL
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")  // WebSocket 연결 URL
                .setAllowedOriginPatterns("*")  // CORS 설정 (개발 환경)
                .withSockJS();  // SockJS 지원 (WebSocket을 지원하지 않는 브라우저 대응)
    }

    /**
     * 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 클라이언트가 메시지를 보낼 때 사용하는 prefix
        registry.setApplicationDestinationPrefixes("/app");

        // 메시지를 받을 때 사용하는 prefix
        registry.enableSimpleBroker("/topic", "/queue");

        // 특정 사용자에게 메시지를 보낼 때 사용하는 prefix
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 인바운드 채널에 인터셉터 등록
     * 클라이언트 -> 서버 메시지에 인터셉터 적용
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(webSocketAuthInterceptor);
    }
}
