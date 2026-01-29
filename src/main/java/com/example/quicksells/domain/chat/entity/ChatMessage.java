package com.example.quicksells.domain.chat.entity;

import com.example.quicksells.common.entity.BaseEntity;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 채팅 메시지 엔티티
 */
@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_room_created", columnList = "chat_room_id, created_at"),
                @Index(name = "idx_sender", columnList = "sender_id"),
                @Index(name = "idx_is_read", columnList = "is_read")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom; // 채팅방

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // 발신자

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 메시지 내용

    @Column(nullable = false, length = 10)
    private Boolean isRead = false; // 읽음 여부

    @Column(nullable = false, length = 10)
    private boolean isDeleted = false; // 삭제 여부

    public ChatMessage(ChatRoom chatRoom, User sender, String content) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.isRead = false;
        this.isDeleted = false;
    }

    // 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }
}
