package com.example.quicksells.domain.chat.service;

import com.example.quicksells.common.enums.*;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.chat.entity.ChatMessage;
import com.example.quicksells.domain.chat.entity.ChatRoom;
import com.example.quicksells.domain.chat.model.request.ChatMessageRequest;
import com.example.quicksells.domain.chat.model.request.ChatRoomCreateRequest;
import com.example.quicksells.domain.chat.model.response.ChatMessageResponse;
import com.example.quicksells.domain.chat.model.response.ChatRoomResponse;
import com.example.quicksells.domain.chat.repository.ChatMessageRepository;
import com.example.quicksells.domain.chat.repository.ChatRoomRepository;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.deal.repository.DealRepository;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final DealRepository dealRepository;

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
     * 구매자-판매자 채팅방 생성 (경매 낙찰 후)
     * - Deal 기반으로 채팅방 생성
     * - 경매 낙찰 확인 필수
     */
    @Transactional
    public ChatRoomResponse createBuyerSellerChatRoom(Long dealId, AuthUser authUser) {
        Long userId = authUser.getId();

        // 1. Deal 조회 (Fetch Join으로 연관 엔티티 함께 조회)
        Deal deal = dealRepository.findByIdWithUsersForChat(dealId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_DEAL));

        // 2. 경매 낙찰 확인
        validateAuctionCompleted(deal);

        // 3. 구매자/판매자 추출
        User buyer = getBuyerFromDeal(deal);
        User seller = getSellerFromDeal(deal);

        // 4. 구매자 또는 판매자인지 확인
        if (!userId.equals(buyer.getId()) && !userId.equals(seller.getId())) {
            throw new CustomException(ExceptionCode.CHAT_BETWEEN_USERS_NOT_ALLOWED);
        }

        // 5. 기존 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(buyer.getId(), seller.getId())
                .orElseGet(() -> {
                    // 6. 없으면 새로 생성 (BUYER_SELLER 타입)
                    ChatRoom newChatRoom = new ChatRoom(buyer, seller, deal);
                    return chatRoomRepository.save(newChatRoom);
                });

        // 7. 응답 생성
        Long unreadCount = chatMessageRepository.countUnreadMessages(chatRoom.getId(), userId);

        log.info("구매자-판매자 채팅방 생성/조회 - Deal: {}, Buyer: {}, Seller: {}", dealId, buyer.getId(), seller.getId());

        return ChatRoomResponse.of(chatRoom, userId, null, unreadCount);
    }

    /**
     * Deal에서 구매자 추출
     * ERD: Deal → Auction → buyer
     */
    private User getBuyerFromDeal(Deal deal) {

        Auction auction = deal.getAuction();

        if (auction == null) {
            throw new CustomException(ExceptionCode.NOT_FOUND_AUCTION);
        }

        User buyer = auction.getBuyer();
        if (buyer == null) {
            throw new CustomException(ExceptionCode.NOT_FOUND_BUYER);
        }

        return buyer;
    }

    /**
     * Deal에서 판매자 추출
     * ERD: Deal → Appraise → Item → seller
     */
    private User getSellerFromDeal(Deal deal) {

        Appraise appraise = deal.getAppraise();
        if (appraise == null) {
            throw new CustomException(ExceptionCode.NOT_FOUND_APPRAISE);
        }

        if (appraise.getItem() == null) {
            throw new CustomException(ExceptionCode.NOT_FOUND_ITEM);
        }

        User seller = appraise.getItem().getSeller();
        if (seller == null) {
            throw new CustomException(ExceptionCode.NOT_FOUND_SELLER);
        }

        return seller;
    }

    /**
     * 경매 낙찰 검증
     * 조건:
     * 1. Appraise.appraiseStatus = AUCTION
     * 2. Auction.auctionStatusType = SUCCESSFUL_BID
     */
    private void validateAuctionCompleted(Deal deal) {

        // 1. Appraise 확인
        Appraise appraise = deal.getAppraise();
        if (appraise == null) {
            throw new CustomException(ExceptionCode.NOT_FOUND_APPRAISE);
        }

        if (appraise.getAppraiseStatus() != AppraiseStatus.AUCTION) {
            log.warn("감정 상태가 경매가 아님 - Appraise: {}, Status: {}", appraise.getId(), appraise.getAppraiseStatus());
            throw new CustomException(ExceptionCode.CHAT_BETWEEN_USERS_NOT_ALLOWED);
        }

        // 2. Auction 확인
        Auction auction = deal.getAuction();
        if (auction == null) {
            throw new CustomException(ExceptionCode.NOT_FOUND_AUCTION);
        }

        if (auction.getStatus() != AuctionStatusType.SUCCESSFUL_BID) {
            log.warn("경매 상태가 낙찰이 아님 - Auction: {}, Status: {}", auction.getId(), auction.getStatus());
            throw new CustomException(ExceptionCode.CHAT_BETWEEN_USERS_NOT_ALLOWED);
        }

    }

    /**
     * 내 채팅방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(AuthUser authUser) {

        Long userId = authUser.getId();

        // 1. 내 채팅방 목록 조회 (FETCH JOIN)
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserIdWithUsers(userId);

        if (chatRooms.isEmpty()) {
            return List.of();
        }

        // 2. 채팅방 ID 리스트 추출
        List<Long> chatRoomIds = chatRooms.stream()
                .map(ChatRoom::getId)
                .collect(Collectors.toList());

        // 3. 안 읽은 메시지 수 일괄 조회 (1번의 쿼리)
        Map<Long, Long> unreadCountMap = chatMessageRepository
                .countUnreadMessagesBatch(chatRoomIds, userId)
                .stream()
                .collect(Collectors.toMap(
                        ChatMessageRepository.UnreadCountProjection::getChatRoomId,
                        ChatMessageRepository.UnreadCountProjection::getUnreadCount
                ));

        // 4. 마지막 메시지 일괄 조회 (1번의 쿼리)
        Map<Long, String> lastMessageMap = chatMessageRepository
                .findLastMessagesBatch(chatRoomIds)
                .stream()
                .collect(Collectors.toMap(
                        msg -> msg.getChatRoom().getId(),
                        ChatMessage::getContent
                ));

        // 5. 응답 생성 (Map에서 조회)
        return chatRooms.stream()
                .map(chatRoom -> {
                    String lastMessage = lastMessageMap.get(chatRoom.getId());
                    Long unreadCount = unreadCountMap.getOrDefault(chatRoom.getId(), 0L);

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

        // 1. 채팅 검증 + ChatRoom 조회 한 번에
        ChatRoom chatRoom = validateAndGetChatRoom(userId, chatRoomId);

        // 2. 발신자 조회
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        // 3. 메시지 생성
        ChatMessage message = new ChatMessage(chatRoom, sender, request.getContent());
        chatMessageRepository.save(message);

        return ChatMessageResponse.from(message);
    }

    /**
     * 채팅 권한 검증 + ChatRoom 반환 (중복 제거)
     */
    private ChatRoom validateAndGetChatRoom(Long userId, Long chatRoomId) {
        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ExceptionCode.CHAT_ROOM_NOT_FOUND));

        // 2. 참여자 확인
        if (!chatRoom.isParticipant(userId)) {
            throw new CustomException(ExceptionCode.CHAT_PERMISSION_DENIED);
        }

        // 3. BUYER_SELLER 타입이면 경매 낙찰 확인
        if (chatRoom.getType() == ChatRoomType.BUYER_SELLER) {
            Deal deal = chatRoom.getDeal();
            if (deal == null) {
                throw new CustomException(ExceptionCode.CHAT_PERMISSION_DENIED);
            }

            validateAuctionCompleted(deal);
        }

        // 4. USER_ADMIN 타입이면 그대로 반환
        return chatRoom;
    }

    /**
     * 채팅 권한 확인 (WebSocket 전용 - boolean 반환)
     */
    @Transactional(readOnly = true)
    public boolean canChat(Long userId, Long chatRoomId) {

        try {
            validateAndGetChatRoom(userId, chatRoomId);
            return true;
        } catch (CustomException e) {
            log.warn("채팅 권한 없음 - User: {}, ChatRoom: {}, Reason: {}", userId, chatRoomId, e.getMessage());
            return false;
        }
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

        // 읽음 처리 - 이미 validateAndGetChatRoom()에서 권한 확인했으므로 검증 생략
        int updatedCount = chatMessageRepository.markAllAsRead(chatRoomId, userId);

    }
}
