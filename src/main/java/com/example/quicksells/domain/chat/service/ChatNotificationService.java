package com.example.quicksells.domain.chat.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.chat.model.response.ChatRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * WebSocket 알림 전송 전용 서비스
 * - 채팅 관련 실시간 알림 처리
 * - SimpMessagingTemplate 의존성을 한 곳에서만 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅방 삭제 알림 전송
     *
     * @param user1Id 참여자1 ID
     * @param user2Id 참여자2 ID
     * @param chatRoomId 삭제된 채팅방 ID
     */
    public void sendRoomDeletedNotification(Long user1Id, Long user2Id, Long chatRoomId) {

        Map<String, Object> message = Map.of("type", "ROOM_DELETED", "chatRoomId", chatRoomId);

        // user1에게 알림
        messagingTemplate.convertAndSendToUser(
                user1Id.toString(),
                "/queue/chatroom-updates",
                message
        );

        // user2에게 알림
        messagingTemplate.convertAndSendToUser(
                user2Id.toString(),
                "/queue/chatroom-updates",
                message
        );
    }

    /**
     * 읽지 않은 메시지 카운트 업데이트 알림
     *
     * @param userId 사용자 ID
     * @param chatRoomId 채팅방 ID
     * @param unreadCount 읽지 않은 메시지 수
     */
    public void sendUnreadCountUpdate(Long userId, Long chatRoomId, Long unreadCount) {

        Map<String, Object> message = Map.of("type", "UNREAD_COUNT", "chatRoomId", chatRoomId, "unreadCount", unreadCount);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/chatroom-updates",
                message
        );

    }

    /**
     * 채팅 메시지 브로드캐스트
     *
     * @param chatRoomId 채팅방 ID
     * @param message 메시지 객체
     */
    public void broadcastMessage(Long chatRoomId, Object message) {

        // 채팅방 ID NULL 방어 코드
        if (chatRoomId == null) {
            throw new CustomException(ExceptionCode.CANNOT_CHATROOM_ID_IS_NULL);
        }

        String destination = "/topic/chat/room/" + chatRoomId;
        messagingTemplate.convertAndSend(destination, message);

    }

    /**
     * 새 채팅방 생성 알림 전송
     */
    public void sendNewChatRoomNotification(Long otherUserId, ChatRoomResponse chatRoom) {

        Map<String, Object> message = Map.of(
                "type", "NEW_ROOM",
                "chatRoomId", chatRoom.getChatRoomId(),
                "chatRoom", chatRoom  //  전체 정보 포함
        );

        messagingTemplate.convertAndSendToUser(
                otherUserId.toString(),
                "/queue/chatroom-updates",
                message
        );

    }

}
