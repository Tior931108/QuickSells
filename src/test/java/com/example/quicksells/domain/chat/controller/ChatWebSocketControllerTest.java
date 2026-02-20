package com.example.quicksells.domain.chat.controller;

import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.common.security.JwtAuthenticationToken;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.chat.model.request.ChatMessageRequest;
import com.example.quicksells.domain.chat.model.response.ChatMessageResponse;
import com.example.quicksells.domain.chat.service.ChatNotificationService;
import com.example.quicksells.domain.chat.service.ChatProfanityFilterService;
import com.example.quicksells.domain.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Principal;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    @Mock
    private ChatService chatService;

    @Mock
    private ChatNotificationService notificationService;

    @Mock
    private ChatProfanityFilterService chatProfanityFilterService;

    private AuthUser authUser;
    private Principal principal;
    private ChatMessageRequest request;
    private ChatMessageResponse response;

    @BeforeEach
    void setUp() {
        // AuthUser 설정
        authUser = new AuthUser(1L, "user@test.com", UserRole.USER, "테스트유저");

        // JwtAuthenticationToken Mock (Principal)
        JwtAuthenticationToken token = mock(JwtAuthenticationToken.class);
        when(token.getPrincipal()).thenReturn(authUser);
        principal = token;

        // ChatMessageRequest 설정
        request = new ChatMessageRequest(1L, "안녕하세요");

        // ChatMessageResponse 설정
        response = new ChatMessageResponse(1L, 1L, 1L, "테스트유저", "안녕하세요", false, LocalDateTime.now());
    }

    @Test
    @DisplayName("WebSocket 메시지 전송 성공")
    void sendMessage_Success() {
        // Given
        String filteredContent = "안녕하세요";

        when(chatProfanityFilterService.filterProfanity("안녕하세요")).thenReturn(filteredContent);
        when(chatService.sendMessageWithoutValidation(eq(1L), any(AuthUser.class), eq(filteredContent))).thenReturn(response);
        doNothing().when(notificationService).broadcastMessage(eq(1L), any(ChatMessageResponse.class));

        // When
        chatWebSocketController.sendMessage(request, principal);

        // Then
        verify(chatProfanityFilterService, times(1)).filterProfanity("안녕하세요");
        verify(chatService, times(1)).sendMessageWithoutValidation(eq(1L), any(AuthUser.class), eq(filteredContent));
        verify(notificationService, times(1)).broadcastMessage(eq(1L), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("비속어 필터링 후 메시지 전송")
    void sendMessage_WithProfanityFilter_Success() {
        // Given
        ChatMessageRequest profaneRequest = new ChatMessageRequest(1L, "나쁜말 포함");
        String filteredContent = "*** 포함";

        when(chatProfanityFilterService.filterProfanity("나쁜말 포함")).thenReturn(filteredContent);

        ChatMessageResponse filteredResponse = new ChatMessageResponse(1L, 1L, 1L, "테스트유저", filteredContent, false, LocalDateTime.now());

        when(chatService.sendMessageWithoutValidation(eq(1L), any(AuthUser.class), eq(filteredContent))).thenReturn(filteredResponse);
        doNothing().when(notificationService).broadcastMessage(eq(1L), any(ChatMessageResponse.class));

        // When
        chatWebSocketController.sendMessage(profaneRequest, principal);

        // Then
        verify(chatProfanityFilterService, times(1)).filterProfanity("나쁜말 포함");
        verify(chatService, times(1)).sendMessageWithoutValidation(eq(1L), any(AuthUser.class), eq(filteredContent));
        verify(notificationService, times(1)).broadcastMessage(eq(1L), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("여러 사용자가 동시에 메시지 전송")
    void sendMessage_MultipleUsers_Success() {
        // Given - User 1
        String filteredContent1 = "안녕하세요";
        when(chatProfanityFilterService.filterProfanity("안녕하세요")).thenReturn(filteredContent1);
        when(chatService.sendMessageWithoutValidation(eq(1L), any(AuthUser.class), eq(filteredContent1))).thenReturn(response);
        doNothing().when(notificationService).broadcastMessage(eq(1L), any(ChatMessageResponse.class));

        // When - User 1 메시지 전송
        chatWebSocketController.sendMessage(request, principal);

        // Given - User 2
        AuthUser authUser2 = new AuthUser(2L, "user2@test.com", UserRole.USER, "테스트유저2");
        JwtAuthenticationToken token2 = mock(JwtAuthenticationToken.class);
        when(token2.getPrincipal()).thenReturn(authUser2);
        Principal principal2 = token2;

        ChatMessageRequest request2 = new ChatMessageRequest(1L, "반갑습니다");
        String filteredContent2 = "반갑습니다";

        ChatMessageResponse response2 = new ChatMessageResponse(2L, 1L, 2L, "테스트유저2", "반갑습니다", false, LocalDateTime.now());

        when(chatProfanityFilterService.filterProfanity("반갑습니다")).thenReturn(filteredContent2);
        when(chatService.sendMessageWithoutValidation(eq(1L), any(AuthUser.class), eq(filteredContent2))).thenReturn(response2);

        // When - User 2 메시지 전송
        chatWebSocketController.sendMessage(request2, principal2);

        // Then - 두 메시지 모두 정상 처리
        verify(chatProfanityFilterService, times(1)).filterProfanity("안녕하세요");
        verify(chatProfanityFilterService, times(1)).filterProfanity("반갑습니다");
        verify(chatService, times(2)).sendMessageWithoutValidation(eq(1L), any(AuthUser.class), any());
        verify(notificationService, times(2)).broadcastMessage(eq(1L), any(ChatMessageResponse.class));
    }

}