package com.example.quicksells.domain.chat.service;

import com.example.quicksells.common.enums.ChatRoomType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.chat.model.response.ChatRoomResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatNotificationService notificationService;

    /**
     * sendRoomDeletedNotification 테스트
     */
    @Test
    @DisplayName("채팅방 삭제 알림을 두 사용자에게 전송한다")
    void sendRoomDeletedNotification_ShouldSendToTwoUsers() {
        // Given
        Long user1Id = 1L;
        Long user2Id = 2L;
        Long chatRoomId = 100L;

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        notificationService.sendRoomDeletedNotification(user1Id, user2Id, chatRoomId);

        // Then
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                anyString(),
                eq("/queue/chatroom-updates"),
                messageCaptor.capture()
        );

        // 메시지 내용 검증
        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage).containsEntry("type", "ROOM_DELETED");
        assertThat(capturedMessage).containsEntry("chatRoomId", chatRoomId);
    }

    @Test
    @DisplayName("채팅방 삭제 알림은 올바른 목적지로 전송된다")
    void sendRoomDeletedNotification_ShouldSendToCorrectDestination() {
        // Given
        Long user1Id = 1L;
        Long user2Id = 2L;
        Long chatRoomId = 100L;

        // When
        notificationService.sendRoomDeletedNotification(user1Id, user2Id, chatRoomId);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq("1"),
                eq("/queue/chatroom-updates"),
                any(Map.class)
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("2"),
                eq("/queue/chatroom-updates"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("채팅방 삭제 알림 메시지 구조가 올바르다")
    void sendRoomDeletedNotification_MessageStructure_IsCorrect() {
        // Given
        Long user1Id = 1L;
        Long user2Id = 2L;
        Long chatRoomId = 100L;

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        notificationService.sendRoomDeletedNotification(user1Id, user2Id, chatRoomId);

        // Then
        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                anyString(),
                anyString(),
                messageCaptor.capture()
        );

        Map<String, Object> message = messageCaptor.getValue();
        assertThat(message).hasSize(2);
        assertThat(message).containsKeys("type", "chatRoomId");
        assertThat(message.get("type")).isEqualTo("ROOM_DELETED");
        assertThat(message.get("chatRoomId")).isEqualTo(100L);
    }

    /**
     * sendUnreadCountUpdate 테스트
     */
    @Test
    @DisplayName("읽지 않은 메시지 카운트 업데이트 알림을 전송한다")
    void sendUnreadCountUpdate_ShouldSendToUser() {
        // Given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long unreadCount = 5L;

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        notificationService.sendUnreadCountUpdate(userId, chatRoomId, unreadCount);

        // Then
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("1"),
                eq("/queue/chatroom-updates"),
                messageCaptor.capture()
        );

        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage).containsEntry("type", "UNREAD_COUNT");
        assertThat(capturedMessage).containsEntry("chatRoomId", chatRoomId);
        assertThat(capturedMessage).containsEntry("unreadCount", unreadCount);
    }

    @Test
    @DisplayName("읽지 않은 메시지 카운트가 0인 경우에도 알림을 전송한다")
    void sendUnreadCountUpdate_WithZeroCount_ShouldSend() {
        // Given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long unreadCount = 0L;

        // When
        notificationService.sendUnreadCountUpdate(userId, chatRoomId, unreadCount);

        // Then
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                anyString(),
                anyString(),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("읽지 않은 메시지 카운트 알림 메시지 구조가 올바른지 확인한다.")
    void sendUnreadCountUpdate_MessageStructure_IsCorrect() {
        // Given
        Long userId = 1L;
        Long chatRoomId = 100L;
        Long unreadCount = 10L;

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        notificationService.sendUnreadCountUpdate(userId, chatRoomId, unreadCount);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                anyString(),
                anyString(),
                messageCaptor.capture()
        );

        Map<String, Object> message = messageCaptor.getValue();
        assertThat(message).hasSize(3);
        assertThat(message).containsKeys("type", "chatRoomId", "unreadCount");
        assertThat(message.get("type")).isEqualTo("UNREAD_COUNT");
        assertThat(message.get("chatRoomId")).isEqualTo(100L);
        assertThat(message.get("unreadCount")).isEqualTo(10L);
    }

    /**
     * broadcastMessage 테스트
     */
    @Test
    @DisplayName("채팅방에 메시지를 브로드캐스트한다")
    void broadcastMessage_ShouldSendToTopic() {
        // Given
        Long chatRoomId = 100L;
        String testMessage = "테스트 메시지";

        // When
        notificationService.broadcastMessage(chatRoomId, testMessage);

        // Then
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/chat/room/100"),
                eq(testMessage)
        );
    }

    @Test
    @DisplayName("브로드캐스트 메시지의 destination이 올바르게 생성된다")
    void broadcastMessage_DestinationFormat_IsCorrect() {
        // Given
        Long chatRoomId = 999L;
        Object message = Map.of("content", "hello");

        // When
        notificationService.broadcastMessage(chatRoomId, message);

        // Then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/chat/room/999"),
                eq(message)
        );
    }

    /**
     * sendNewChatRoomNotification 테스트
     */
    @Test
    @DisplayName("새 채팅방 생성 알림을 상대방에게 전송한다")
    void sendNewChatRoomNotification_ShouldSendToOtherUser() {
        // Given
        Long otherUserId = 2L;
        ChatRoomResponse chatRoom = createChatRoomResponse(100L);

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        notificationService.sendNewChatRoomNotification(otherUserId, chatRoom);

        // Then
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("2"),
                eq("/queue/chatroom-updates"),
                messageCaptor.capture()
        );

        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage).containsEntry("type", "NEW_ROOM");
        assertThat(capturedMessage).containsEntry("chatRoomId", 100L);
        assertThat(capturedMessage).containsEntry("chatRoom", chatRoom);
    }

    @Test
    @DisplayName("새 채팅방 알림 메시지 구조가 올바르다")
    void sendNewChatRoomNotification_MessageStructure_IsCorrect() {
        // Given
        Long otherUserId = 2L;
        ChatRoomResponse chatRoom = createChatRoomResponse(200L);

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        notificationService.sendNewChatRoomNotification(otherUserId, chatRoom);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                anyString(),
                anyString(),
                messageCaptor.capture()
        );

        Map<String, Object> message = messageCaptor.getValue();
        assertThat(message).hasSize(3);
        assertThat(message).containsKeys("type", "chatRoomId", "chatRoom");
        assertThat(message.get("type")).isEqualTo("NEW_ROOM");
        assertThat(message.get("chatRoomId")).isEqualTo(200L);
        assertThat(message.get("chatRoom")).isInstanceOf(ChatRoomResponse.class);
    }

    @Test
    @DisplayName("새 채팅방 알림은 ChatRoomResponse 전체 정보를 포함한다")
    void sendNewChatRoomNotification_IncludesFullChatRoomInfo() {
        // Given
        Long otherUserId = 3L;
        ChatRoomResponse chatRoom = createChatRoomResponse(300L);

        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        notificationService.sendNewChatRoomNotification(otherUserId, chatRoom);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq("3"),
                eq("/queue/chatroom-updates"),
                messageCaptor.capture()
        );

        Map<String, Object> message = messageCaptor.getValue();
        ChatRoomResponse capturedChatRoom = (ChatRoomResponse) message.get("chatRoom");

        assertThat(capturedChatRoom).isNotNull();
        assertThat(capturedChatRoom.getChatRoomId()).isEqualTo(300L);
        assertThat(capturedChatRoom.getType()).isEqualTo(ChatRoomType.USER_ADMIN);
        assertThat(capturedChatRoom.getOtherUser()).isNotNull();
        assertThat(capturedChatRoom.getOtherUser().getUserId()).isEqualTo(1L);
        assertThat(capturedChatRoom.getOtherUser().getName()).isEqualTo("홍길동");
        assertThat(capturedChatRoom.getOtherUser().getRole()).isEqualTo("USER");
    }


    @Test
    @DisplayName("새 채팅방 알림은 올바른 사용자에게 전송된다")
    void sendNewChatRoomNotification_SendsToCorrectUser() {
        // Given
        Long otherUserId = 5L;
        ChatRoomResponse chatRoom = createChatRoomResponse(500L);

        // When
        notificationService.sendNewChatRoomNotification(otherUserId, chatRoom);

        // Then
        verify(messagingTemplate).convertAndSendToUser(
                eq("5"),  // userId를 String으로 변환
                eq("/queue/chatroom-updates"),
                any(Map.class)
        );
    }

    /**
     * 통합 시나리오 테스트
     */
    @Test
    @DisplayName("여러 알림을 순차적으로 전송할 수 있다")
    void multipleNotifications_ShouldWorkSequentially() {
        // Given
        Long userId = 1L;
        Long chatRoomId = 100L;
        ChatRoomResponse chatRoom = createChatRoomResponse(chatRoomId);

        // When
        notificationService.sendUnreadCountUpdate(userId, chatRoomId, 5L);
        notificationService.sendRoomDeletedNotification(1L, 2L, chatRoomId);
        notificationService.broadcastMessage(chatRoomId, "test");
        notificationService.sendNewChatRoomNotification(userId, chatRoom);

        // Then
        verify(messagingTemplate, times(4)).convertAndSendToUser(
                anyString(),
                anyString(),
                any()
        );
        verify(messagingTemplate, times(1)).convertAndSend(
                anyString(),
                (Object) any()
        );
    }

    /**
     * Null 처리 테스트
     */
    @Test
    @DisplayName("null chatRoomId로 브로드캐스트시 NullPointerException 발생")
    void broadcastMessage_WithNullChatRoomId_ShouldThrowException() {
        // Given
        Long chatRoomId = null;
        String message = "test";

        // When & Then
        assertThrows(
                CustomException.class,
                () -> notificationService.broadcastMessage(chatRoomId, message)
        );
    }

    /**
     * 테스트 헬퍼 메서드
     */
    private ChatRoomResponse createChatRoomResponse(Long chatRoomId) {
        // OtherUserDto 생성
        ChatRoomResponse.OtherUserDto otherUser = new ChatRoomResponse.OtherUserDto(
                1L,
                "홍길동",
                "USER"
        );

        // ChatRoomResponse 생성
        return new ChatRoomResponse(
                chatRoomId,
                ChatRoomType.USER_ADMIN,
                otherUser,
                "마지막 메시지",
                0L,
                LocalDateTime.now()
        );
    }
}