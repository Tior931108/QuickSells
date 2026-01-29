package com.example.quicksells.domain.chat.model.response;

import com.example.quicksells.domain.chat.entity.ChatMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ChatMessageResponse {

    private final Long messageId;  // 메시지 ID
    private final Long chatRoomId; // 채팅방 ID
    private final Long senderId;   // 발신자 ID
    private final String senderName;  // 발신자 이름
    private final String content;  // 메시지 내용
    private final Boolean isRead;  // 읽음 여부
    private final LocalDateTime createdAt; // 전송 시간

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getContent(),
                message.getIsRead(),
                message.getCreatedAt()
        );
    }
}
