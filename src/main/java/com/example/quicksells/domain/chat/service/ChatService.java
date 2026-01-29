package com.example.quicksells.domain.chat.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.chat.entity.ChatMessage;
import com.example.quicksells.domain.chat.entity.ChatRoom;
import com.example.quicksells.domain.chat.model.request.ChatMessageRequest;
import com.example.quicksells.domain.chat.model.request.ChatRoomCreateRequest;
import com.example.quicksells.domain.chat.model.response.ChatMessageResponse;
import com.example.quicksells.domain.chat.model.response.ChatRoomResponse;
import com.example.quicksells.domain.chat.repository.ChatMessageRepository;
import com.example.quicksells.domain.chat.repository.ChatRoomRepository;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    /**
     * 채팅방 생성 또는 조회
     * - 이미 존재하면 기존 채팅방 반환
     * - 없으면 새로 생성
     * - 유저-유저 채팅 차단
     */
    @Transactional
    public ChatRoomResponse createOrGetChatRoom(ChatRoomCreateRequest request, AuthUser authUser) {

        Long currentUserId = authUser.getId();
        Long otherUserId = request.getOtherUserId();

        // 1. 자기 자신과 채팅 시도 차단
        if (currentUserId.equals(otherUserId)) {
            throw new CustomException(ExceptionCode.PRINCIPAL_CHAT_PERMISSION_DENIED);
        }

        // 2. 상대방 조회
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        // 3. 현재 사용자 조회
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        // 4. 유저-유저 채팅 차단 검증
        validateUserToUserChat(currentUser, otherUser);

        // 5. 기존 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(currentUserId, otherUserId)
                .orElseGet(() -> {
                    // 6. 없으면 새로 생성 (USER_ADMIN 타입)
                    ChatRoom newChatRoom = new ChatRoom(currentUser, otherUser);
                    return chatRoomRepository.save(newChatRoom);
                });

        // 7. 응답 생성
        Long unreadCount = chatMessageRepository.countUnreadMessages(chatRoom.getId(), currentUserId);

        return ChatRoomResponse.of(chatRoom, currentUserId, null, unreadCount);
    }

    /**
     * 유저-유저 채팅 차단 검증
     * - 둘 다 일반 사용자(USER)면 차단
     * - 한 명이라도 관리자(ADMIN, APPRAISER)면 허용
     */
    private void validateUserToUserChat(User user1, User user2) {

        UserRole role1 = user1.getRole();
        UserRole role2 = user2.getRole();

        // 둘 다 일반 사용자인 경우
        if (role1 == UserRole.USER && role2 == UserRole.USER) {
            throw new CustomException(ExceptionCode.CHAT_BETWEEN_USERS_NOT_ALLOWED);
        }
    }

    /**
     * 내 채팅방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(AuthUser authUser) {
        Long userId = authUser.getId();

        List<ChatRoom> chatRooms = chatRoomRepository.findByUserId(userId);

        return chatRooms.stream()
                .map(chatRoom -> {
                    // 마지막 메시지 조회
                    Page<ChatMessage> lastMessages = chatMessageRepository.findByChatRoomId(
                            chatRoom.getId(),
                            Pageable.ofSize(1)
                    );
                    String lastMessage = lastMessages.hasContent() ? lastMessages.getContent().get(0).getContent() : null;

                    // 안 읽은 메시지 수
                    Long unreadCount = chatMessageRepository.countUnreadMessages(
                            chatRoom.getId(),
                            userId
                    );

                    return ChatRoomResponse.of(chatRoom, userId, lastMessage, unreadCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 채팅방 상세 조회
     */
    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoomDetail(Long chatRoomId, AuthUser authUser) {

        Long userId = authUser.getId();

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND));

        // 참여자 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new CustomException(ExceptionCode.CHAT_PERMISSION_DENIED);
        }

        Long unreadCount = chatMessageRepository.countUnreadMessages(chatRoomId, userId);

        return ChatRoomResponse.of(chatRoom, userId, null, unreadCount);
    }

    /**
     * 메시지 전송
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long chatRoomId, ChatMessageRequest request, AuthUser authUser) {

        Long userId = authUser.getId();

        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND));

        // 2. 참여자 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new CustomException(ExceptionCode.CHAT_PERMISSION_DENIED);
        }

        // 3. 발신자 조회
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        // 4. 메시지 생성
        ChatMessage message = new ChatMessage(chatRoom, sender, request.getContent());
        chatMessageRepository.save(message);


        return ChatMessageResponse.from(message);
    }

    /**
     * 메시지 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(Long chatRoomId, Pageable pageable, AuthUser authUser) {

        Long userId = authUser.getId();

        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND));

        // 2. 참여자 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new CustomException(ExceptionCode.CHAT_PERMISSION_DENIED);
        }

        // 3. 메시지 조회
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(chatRoomId, pageable);

        return messages.map(ChatMessageResponse::from);
    }

    /**
     * 메시지 읽음 처리
     */
    @Transactional
    public void markMessagesAsRead(Long chatRoomId, AuthUser authUser) {

        Long userId = authUser.getId();

        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND));

        // 2. 참여자 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new CustomException(ExceptionCode.CHAT_PERMISSION_DENIED);
        }

        // 3. 읽음 처리
        int updatedCount = chatMessageRepository.markAllAsRead(chatRoomId, userId);

    }
}
