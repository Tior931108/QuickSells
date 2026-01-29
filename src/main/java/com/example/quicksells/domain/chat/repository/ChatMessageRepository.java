package com.example.quicksells.domain.chat.repository;

import com.example.quicksells.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 채팅방의 메시지 조회 (페이징)
    @Query("SELECT cm FROM ChatMessage cm " +
            "WHERE cm.chatRoom.id = :chatRoomId " +
            "ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    // 안 읽은 메시지 개수
    @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
            "WHERE cm.chatRoom.id = :chatRoomId " +
            "  AND cm.sender.id != :userId " +
            "  AND cm.isRead = false")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    // 채팅방의 모든 메시지 읽음 처리
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isRead = true " +
            "WHERE cm.chatRoom.id = :chatRoomId " +
            "  AND cm.sender.id != :userId " +
            "  AND cm.isRead = false")
    int markAllAsRead(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}
