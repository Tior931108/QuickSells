package com.example.quicksells.domain.chat.controller;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.common.security.JwtAuthenticationToken;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.chat.model.request.ChatMessageRequest;
import com.example.quicksells.domain.chat.model.response.ChatMessageResponse;
import com.example.quicksells.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 채팅 컨트롤러
 * - STOMP 프로토콜을 통한 실시간 메시지 송수신
 * - 채팅방별 구독 관리
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 실시간 메시지 전송
     *
     * 클라이언트 요청: SEND /app/chat/message
     * 브로드캐스트: /topic/chat/room/{chatRoomId}
     *
     * @param request 메시지 요청 (chatRoomId, content 포함)
     * @param principal 인증된 사용자 정보
     */
    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {

        try {
            // 1. 인증 정보 추출
            AuthUser authUser = extractAuthUser(principal);

            // 2. 메시지 저장 (ChatService 활용)
            ChatMessageResponse response = chatService.sendMessage(request.getChatRoomId(), request, authUser);

            // 3. 채팅방 구독자들에게 브로드캐스트
            String destination = "/topic/chat/room/" + request.getChatRoomId();
            messagingTemplate.convertAndSend(destination, response);

        } catch (Exception e) {
            // 에러는 클라이언트로 전달되지 않으므로 로깅만 수행
            // 필요 시 에러 응답을 별도 채널로 전송 가능
            log.error("메시지 전송 실패: {}", e.getMessage(), e);
        }
    }


    /**
     * Principal에서 AuthUser 추출
     */
    private AuthUser extractAuthUser(Principal principal) {

        if (principal == null) {
            throw new CustomException(ExceptionCode.NOT_FOUND_PRINCIPAL);
        }

        if (!(principal instanceof JwtAuthenticationToken)) {
            throw new CustomException(ExceptionCode.WRONG_PRINCIPAL_TYPE);
        }

        JwtAuthenticationToken token = (JwtAuthenticationToken) principal;
        Object principalObj = token.getPrincipal();

        if (!(principalObj instanceof AuthUser)) {
            throw new CustomException(ExceptionCode.NOT_SAME_PRINCIPAL_AUTH_USER);
        }

        return (AuthUser) principalObj;
    }

}
