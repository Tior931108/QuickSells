package com.example.quicksells.domain.chat.model.response;

import com.example.quicksells.common.enums.ChatRoomType;
import com.example.quicksells.domain.chat.entity.ChatRoom;
import com.example.quicksells.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ChatRoomResponse {

    private final Long chatRoomId;  // 채팅방 ID
    private final ChatRoomType type;  // 채팅방 타입
    private final OtherUserDto otherUser; // 상대방 정보
    private final String lastMessage;  // 마지막 메시지
    private final Long unreadCount;  // 안 읽은 메시지 수
    private final LocalDateTime updatedAt; // 마지막 업데이트 시간

    /**
     * 정적 팩토리 메소드
     * of : 입력 매개변수를 변환하지 않고 객체를 바로 생성하는 정적 메소드
     * from : 입력 매개변수의 데이터를 파싱하거나 변환하여 객체를 생성하는 정적 메소드
     */
    public static ChatRoomResponse of(ChatRoom chatRoom, Long currentUserId, String lastMessage, Long unreadCount) {

        User otherUser = chatRoom.getOtherUser(currentUserId);

        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getType(),
                OtherUserDto.from(otherUser),
                lastMessage,
                unreadCount,
                chatRoom.getUpdatedAt()
        );
    }

    @Getter
    @RequiredArgsConstructor
    public static class OtherUserDto {
        private final Long userId;
        private final String name;
        private final String role;

        public static OtherUserDto from(User user) {
            return new OtherUserDto(
                    user.getId(),
                    user.getName(),
                    user.getRole().name()
            );
        }
    }
}
