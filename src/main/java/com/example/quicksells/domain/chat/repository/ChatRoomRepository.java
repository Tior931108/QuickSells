package com.example.quicksells.domain.chat.repository;

import com.example.quicksells.common.enums.ChatRoomType;
import com.example.quicksells.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 두 사용자 간의 채팅방 찾기
     * (user1, user2 순서 무관)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE (cr.user1.id = :userId1 AND cr.user2.id = :userId2) " +
            "   OR (cr.user1.id = :userId2 AND cr.user2.id = :userId1)")
    Optional<ChatRoom> findByTwoUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // 특정 사용자가 참여한 모든 채팅방 조회
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.user1.id = :userId OR cr.user2.id = :userId " +
            "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);

    // 특정 타입의 채팅방만 조회
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE (cr.user1.id = :userId OR cr.user2.id = :userId) " +
            "  AND cr.type = :type " +
            "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findByUserIdAndType(@Param("userId") Long userId, @Param("type") ChatRoomType type);

    /**
     * 사용자 ID로 채팅방 조회 (Fetch Join)
     * - 나가지 않은 채팅방만 조회
     */
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
            "LEFT JOIN FETCH cr.user1 " +
            "LEFT JOIN FETCH cr.user2 " +
            "LEFT JOIN FETCH cr.deal " +
            "WHERE cr.isDeleted = false " +
            "AND (cr.user1.id = :userId OR cr.user2.id = :userId) " +
            "AND NOT (" +
            "  (cr.user1.id = :userId AND cr.user1Left = true) OR " +
            "  (cr.user2.id = :userId AND cr.user2Left = true)" +
            ") " +
            "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findByUserIdWithUsers(@Param("userId") Long userId);

    /**
     * Soft delete된 채팅방 조회 (USER_ADMIN 타입)
     * - @SQLRestriction을 우회하기 위해 네이티브 쿼리 사용
     * - isDeleted = true인 채팅방을 찾음
     */
    @Query(value = "SELECT * FROM chat_rooms " +
            "WHERE is_deleted = true " +
            "  AND type = 'USER_ADMIN' " +
            "  AND ((user1_id = :userId1 AND user2_id = :userId2) " +
            "       OR (user1_id = :userId2 AND user2_id = :userId1))",
            nativeQuery = true)
    Optional<ChatRoom> findDeletedByTwoUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Soft delete된 채팅방 조회 (BUYER_SELLER 타입)
     * - isDeleted = true이고 특정 deal에 연결된 채팅방 찾기
     */
    @Query(value = "SELECT * FROM chat_rooms " +
            "WHERE is_deleted = true " +
            "  AND type = 'BUYER_SELLER' " +
            "  AND deal_id = :dealId " +
            "  AND ((user1_id = :userId1 AND user2_id = :userId2) " +
            "       OR (user1_id = :userId2 AND user2_id = :userId1))",
            nativeQuery = true)
    Optional<ChatRoom> findDeletedByTwoUsersAndDeal(@Param("userId1") Long userId1, @Param("userId2") Long userId2, @Param("dealId") Long dealId);
}
