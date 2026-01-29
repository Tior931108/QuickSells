package com.example.quicksells.domain.chat.entity;

import com.example.quicksells.common.entity.BaseEntity;
import com.example.quicksells.common.enums.ChatRoomType;
import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 채팅방 엔티티
 * - 사용자간 1:1 채팅방
 * - 타입에 따라 생성 조건이 다름
 */
@Entity
@Table(name = "chat_rooms",
        // (user1_id, user2_id) 조합으로 중복 채팅방 방지
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_users",
                        columnNames = {"user1_id", "user2_id"}
                )
        },
        indexes = {
                @Index(name = "idx_user1", columnList = "user1_id"),
                @Index(name = "idx_user2", columnList = "user2_id"),
                @Index(name = "idx_type", columnList = "type")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomType type; // 채팅방 타입

    /**
     * 참여자 1 (항상 숫자가 작은 사용자)
     * - USER_ADMIN: 일반 사용자
     * - BUYER_SELLER: 구매자 또는 판매자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    /**
     * 참여자 2 (항상 숫자가 큰 사용자)
     * - USER_ADMIN: 관리자(감정사)
     * - BUYER_SELLER: 판매자 또는 구매자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    /**
     * 연관된 거래 (BUYER_SELLER 타입만)
     * - 경매 낙찰 후 생성된 거래
     * - Deal을 통해 Appraise, Auction 정보 접근 가능
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    @Column(nullable = false, length = 10)
    private boolean isDeleted = false; // 삭제 여부

    /**
     * 생성자 - USER_ADMIN 타입
     *
     * @param user1 일반 사용자
     * @param user2 관리자(감정사)
     */
    public ChatRoom(User user1, User user2) {

        // user1이 항상 작은 ID를 갖도록 정렬 - 채팅방 중복 방지
        if (user1.getId() > user2.getId()) {
            this.user1 = user2;
            this.user2 = user1;
        } else {
            this.user1 = user1;
            this.user2 = user2;
        }
        this.type = ChatRoomType.USER_ADMIN;
        this.deal = null;
        this.isDeleted = false;
    }

    /**
     * 생성자 - BUYER_SELLER 타입
     *
     * @param user1 구매자 또는 판매자
     * @param user2 판매자 또는 구매자
     * @param deal 관련 거래
     */
    public ChatRoom(User user1, User user2, Deal deal) {

        // user1이 항상 작은 ID를 갖도록 정렬
        if (user1.getId() > user2.getId()) {
            this.user1 = user2;
            this.user2 = user1;
        } else {
            this.user1 = user1;
            this.user2 = user2;
        }
        this.type = ChatRoomType.BUYER_SELLER;
        this.deal = deal;
        this.isDeleted = false;
    }

    // 특정 사용자가 이 채팅방의 참여자인지 확인
    public boolean isParticipant(Long userId) {

        return user1.getId().equals(userId) || user2.getId().equals(userId);
    }

    // 상대방 사용자 가져오기
    public User getOtherUser(Long userId) {

        if (user1.getId().equals(userId)) {
            return user2;
        } else if (user2.getId().equals(userId)) {
            return user1;
        }

        throw new CustomException(ExceptionCode.NOT_MATCHED_CHAT_USER);
    }
}
