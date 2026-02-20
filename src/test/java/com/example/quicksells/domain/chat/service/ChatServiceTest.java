package com.example.quicksells.domain.chat.service;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.common.enums.ChatRoomType;
import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.chat.entity.ChatMessage;
import com.example.quicksells.domain.chat.entity.ChatRoom;
import com.example.quicksells.domain.chat.model.request.ChatMessageRequest;
import com.example.quicksells.domain.chat.model.request.ChatRoomCreateRequest;
import com.example.quicksells.domain.chat.model.response.AvailableUsersResponse;
import com.example.quicksells.domain.chat.model.response.ChatMessageResponse;
import com.example.quicksells.domain.chat.model.response.ChatRoomResponse;
import com.example.quicksells.domain.chat.repository.ChatMessageRepository;
import com.example.quicksells.domain.chat.repository.ChatRoomRepository;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.deal.repository.DealRepository;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 사용되지 않는 stubbing에 대해 경고하지 않음
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DealRepository dealRepository;

    @Mock
    private ChatNotificationService notificationService;

    private AuthUser authUser;
    private User currentUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // AuthUser 생성
        authUser = new AuthUser(1L, "user@test.com", UserRole.USER, "테스트유저");

        // User는 Mock만 생성, stubbing은 각 테스트에서
        currentUser = mock(User.class);
        adminUser = mock(User.class);
    }

    /**
     * User Mock 설정 헬퍼 메서드
     * - 테스트 스텁(Test Stub)은 테스트 호출 중 테스트 스텁은 테스트 중에 만들어진 호출에 대해 미리 준비된 답변을 제공하는 것
     * - 만들어진 mock 객체의 메소드를 실행했을 때 어떤 리턴 값을 리턴할지를 정의하는 것
     */
    private void setupUserMocks() {
        // currentUser stubbing
        when(currentUser.getId()).thenReturn(1L);
        when(currentUser.getEmail()).thenReturn("user@test.com");
        when(currentUser.getName()).thenReturn("테스트유저");
        when(currentUser.getRole()).thenReturn(UserRole.USER);

        // adminUser stubbing
        when(adminUser.getId()).thenReturn(2L);
        when(adminUser.getEmail()).thenReturn("admin@test.com");
        when(adminUser.getName()).thenReturn("관리자");
        when(adminUser.getRole()).thenReturn(UserRole.ADMIN);
    }

    /**
     * ChatRoom Mock 설정 헬퍼 메서드
     */
    private ChatRoom createMockChatRoom(Long chatRoomId, User user1, User user2) {
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(chatRoom.getId()).thenReturn(chatRoomId);
        when(chatRoom.getUser1()).thenReturn(user1);
        when(chatRoom.getUser2()).thenReturn(user2);
        when(chatRoom.getType()).thenReturn(ChatRoomType.USER_ADMIN);
        when(chatRoom.getUpdatedAt()).thenReturn(LocalDateTime.now());

        // 양방향 getOtherUser stubbing
        when(chatRoom.getOtherUser(user1.getId())).thenReturn(user2);
        when(chatRoom.getOtherUser(user2.getId())).thenReturn(user1);

        return chatRoom;
    }

    /**
     * Deal Mock 생성 헬퍼 메서드
     */
    private Deal createMockDeal(Long dealId, User buyer, User seller) {
        return createMockDealWithStatus(dealId, buyer, seller, AuctionStatusType.SUCCESSFUL_BID);
    }

    private Deal createMockDealWithStatus(Long dealId, User buyer, User seller, AuctionStatusType status) {
        Deal deal = mock(Deal.class);
        when(deal.getId()).thenReturn(dealId);

        // Auction 설정
        Auction auction = mock(Auction.class);
        when(auction.getBuyer()).thenReturn(buyer);
        when(auction.getStatus()).thenReturn(status);
        when(deal.getAuction()).thenReturn(auction);

        // Appraise 설정
        Appraise appraise = mock(Appraise.class);
        when(appraise.getAppraiseStatus()).thenReturn(AppraiseStatus.AUCTION);

        Item item = mock(Item.class);
        when(item.getSeller()).thenReturn(seller);
        when(appraise.getItem()).thenReturn(item);
        when(deal.getAppraise()).thenReturn(appraise);

        return deal;
    }

    /**
     * createOrGetChatRoom - 정상 케이스
     */
    @Test
    @DisplayName("새 채팅방을 생성한다")
    void createOrGetChatRoom_NewRoom_Success() {
        // Given
        setupUserMocks();  // 필요한 곳에서만 호출!

        ChatRoomCreateRequest request = new ChatRoomCreateRequest(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(chatRoomRepository.findByTwoUsers(1L, 2L)).thenReturn(Optional.empty());
        when(chatRoomRepository.findDeletedByTwoUsers(1L, 2L)).thenReturn(Optional.empty());

        ChatRoom newChatRoom = createMockChatRoom(100L, currentUser, adminUser);

        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(newChatRoom);
        when(chatMessageRepository.countUnreadMessages(any(), eq(1L))).thenReturn(0L);

        // When
        ChatRoomResponse response = chatService.createOrGetChatRoom(request, authUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getChatRoomId()).isEqualTo(100L);
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(notificationService).sendNewChatRoomNotification(eq(2L), any(ChatRoomResponse.class));
    }

    @Test
    @DisplayName("기존 채팅방이 있으면 조회한다")
    void createOrGetChatRoom_ExistingRoom_Success() {
        // Given
        setupUserMocks();
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(2L);

        ChatRoom existingRoom = createMockChatRoom(100L, currentUser, adminUser);
        when(existingRoom.isVisibleTo(1L)).thenReturn(true);

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(chatRoomRepository.findByTwoUsers(1L, 2L)).thenReturn(Optional.of(existingRoom));
        when(chatMessageRepository.countUnreadMessages(any(), eq(1L))).thenReturn(0L);

        // When
        ChatRoomResponse response = chatService.createOrGetChatRoom(request, authUser);

        // Then
        assertThat(response).isNotNull();
        verify(chatRoomRepository, never()).save(any());
        verify(notificationService, never()).sendNewChatRoomNotification(any(), any());
    }

    @Test
    @DisplayName("자기 자신과 채팅 시도시 예외 발생")
    void createOrGetChatRoom_SelfChat_ThrowException() {
        // Given
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(1L);

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.createOrGetChatRoom(request, authUser));
    }

    @Test
    @DisplayName("유저-유저 간 채팅 시도시 예외 발생")
    void createOrGetChatRoom_UserToUser_ThrowException() {
        // Given
        setupUserMocks();
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(2L);

        User anotherUser = mock(User.class);
        when(anotherUser.getId()).thenReturn(2L);
        when(anotherUser.getRole()).thenReturn(UserRole.USER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.createOrGetChatRoom(request, authUser));
    }

    @Test
    @DisplayName("존재하지 않는 사용자와 채팅 시도시 예외 발생")
    void createOrGetChatRoom_UserNotFound_ThrowException() {
        // Given
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.createOrGetChatRoom(request, authUser));
    }

    @Test
    @DisplayName("삭제된 채팅방을 재활성화한다")
    void createOrGetChatRoom_ReactivateDeletedRoom_Success() {
        // Given
        setupUserMocks();
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(2L);

        ChatRoom deletedRoom = createMockChatRoom(100L, currentUser, adminUser);
        when(deletedRoom.isDeleted()).thenReturn(false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(chatRoomRepository.findByTwoUsers(1L, 2L)).thenReturn(Optional.empty());
        when(chatRoomRepository.findDeletedByTwoUsers(1L, 2L)).thenReturn(Optional.of(deletedRoom));
        when(chatMessageRepository.countUnreadMessages(any(), eq(1L))).thenReturn(0L);

        // When
        ChatRoomResponse response = chatService.createOrGetChatRoom(request, authUser);

        // Then
        assertThat(response).isNotNull();
        verify(deletedRoom).reactivate();
        verify(chatRoomRepository, never()).save(any());
    }

    /**
     * sendMessage 테스트
     */
    @Test
    @DisplayName("메시지를 전송한다")
    void sendMessage_Success() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;
        ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoom.isParticipant(1L)).thenReturn(true);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        ChatMessage savedMessage = mock(ChatMessage.class);
        when(savedMessage.getId()).thenReturn(1L);
        when(savedMessage.getContent()).thenReturn("안녕하세요");
        when(savedMessage.getSender()).thenReturn(currentUser);
        when(savedMessage.getChatRoom()).thenReturn(chatRoom);
        when(savedMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        // When
        ChatMessageResponse response = chatService.sendMessage(chatRoomId, request, authUser);

        // Then
        assertThat(response).isNotNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("채팅방이 없으면 예외 발생")
    void sendMessage_ChatRoomNotFound_ThrowException() {
        // Given
        Long chatRoomId = 999L;
        ChatMessageRequest request = new ChatMessageRequest("안녕하세요");
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.sendMessage(chatRoomId, request, authUser));
    }

    @Test
    @DisplayName("참여자가 아니면 예외 발생")
    void sendMessage_NotParticipant_ThrowException() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;
        ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoom.isParticipant(1L)).thenReturn(false);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.sendMessage(chatRoomId, request, authUser));
    }

    /**
     * getMyChatRooms 테스트
     */
    @Test
    @DisplayName("내 채팅방 목록을 조회한다")
    void getMyChatRooms_Success() {
        // Given
        setupUserMocks();
        ChatRoom chatRoom1 = createMockChatRoom(1L, currentUser, adminUser);
        when(chatRoom1.isVisibleTo(1L)).thenReturn(true);

        List<ChatRoom> chatRooms = List.of(chatRoom1);

        when(chatRoomRepository.findByUserIdWithUsers(1L)).thenReturn(chatRooms);
        when(chatMessageRepository.countUnreadMessagesBatch(any(), eq(1L))).thenReturn(List.of());
        when(chatMessageRepository.findLastMessagesBatch(any())).thenReturn(List.of());

        // When
        List<ChatRoomResponse> responses = chatService.getMyChatRooms(authUser);

        // Then
        assertThat(responses).isNotNull();
        verify(chatRoomRepository).findByUserIdWithUsers(1L);
    }

    @Test
    @DisplayName("채팅방이 없으면 빈 리스트 반환")
    void getMyChatRooms_EmptyList_Success() {
        // Given
        when(chatRoomRepository.findByUserIdWithUsers(1L)).thenReturn(List.of());

        // When
        List<ChatRoomResponse> responses = chatService.getMyChatRooms(authUser);

        // Then
        assertThat(responses).isEmpty();
    }

    /**
     * leaveChatRoom 테스트
     */
    @Test
    @DisplayName("채팅방을 나간다")
    void leaveChatRoom_Success() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoom.isParticipant(1L)).thenReturn(true);
        when(chatRoom.isDeleted()).thenReturn(false);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // When
        chatService.leaveChatRoom(chatRoomId, authUser);

        // Then
        verify(chatRoom).leave(1L);
        verify(notificationService, never()).sendRoomDeletedNotification(any(), any(), any());
    }

    @Test
    @DisplayName("양쪽 다 나가면 삭제 알림 전송")
    void leaveChatRoom_BothLeft_SendNotification() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoom.isParticipant(1L)).thenReturn(true);
        when(chatRoom.isDeleted()).thenReturn(true);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // When
        chatService.leaveChatRoom(chatRoomId, authUser);

        // Then
        verify(chatRoom).leave(1L);
        verify(notificationService).sendRoomDeletedNotification(1L, 2L, chatRoomId);
    }

    @Test
    @DisplayName("참여자가 아니면 예외 발생")
    void leaveChatRoom_NotParticipant_ThrowException() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoom.isParticipant(1L)).thenReturn(false);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.leaveChatRoom(chatRoomId, authUser));
    }

    /**
     * markMessagesAsRead 테스트
     */
    @Test
    @DisplayName("메시지를 읽음 처리한다")
    void markMessagesAsRead_Success() {
        // Given
        Long chatRoomId = 1L;
        when(chatMessageRepository.markAllAsRead(chatRoomId, 1L)).thenReturn(5);

        // When
        chatService.markMessagesAsRead(chatRoomId, authUser);

        // Then
        verify(chatMessageRepository).markAllAsRead(chatRoomId, 1L);
        verify(notificationService).sendUnreadCountUpdate(1L, chatRoomId, 0L);
    }

    @Test
    @DisplayName("읽을 메시지가 없으면 알림 전송하지 않음")
    void markMessagesAsRead_NoUnread_NoNotification() {
        // Given
        Long chatRoomId = 1L;
        when(chatMessageRepository.markAllAsRead(chatRoomId, 1L)).thenReturn(0);

        // When
        chatService.markMessagesAsRead(chatRoomId, authUser);

        // Then
        verify(notificationService, never()).sendUnreadCountUpdate(any(), any(), any());
    }

    /**
     * getChatRoomDetail 테스트
     */
    @Test
    @DisplayName("채팅방 상세를 조회한다")
    void getChatRoomDetail_Success() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoom.isParticipant(1L)).thenReturn(true);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.countUnreadMessages(chatRoomId, 1L)).thenReturn(3L);

        // When
        ChatRoomResponse response = chatService.getChatRoomDetail(chatRoomId, authUser);

        // Then
        assertThat(response).isNotNull();
        verify(chatMessageRepository).countUnreadMessages(chatRoomId, 1L);
    }

    @Test
    @DisplayName("참여자가 아니면 예외 발생")
    void getChatRoomDetail_NotParticipant_ThrowException() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoom.isParticipant(1L)).thenReturn(false);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.getChatRoomDetail(chatRoomId, authUser));
    }

    /**
     * sendMessageWithoutValidation 테스트
     */
    @Test
    @DisplayName("검증 없이 메시지를 전송한다")
    void sendMessageWithoutValidation_Success() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;
        String message = "필터링된 메시지";

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        ChatMessage savedMessage = mock(ChatMessage.class);
        when(savedMessage.getId()).thenReturn(1L);
        when(savedMessage.getContent()).thenReturn(message);
        when(savedMessage.getSender()).thenReturn(currentUser);
        when(savedMessage.getChatRoom()).thenReturn(chatRoom);
        when(savedMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        // When
        ChatMessageResponse response = chatService.sendMessageWithoutValidation(
                chatRoomId, authUser, message);

        // Then
        assertThat(response).isNotNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    /**
     * canChat 테스트
     */
    @Test
    @DisplayName("채팅 권한이 있으면 true 반환")
    void canChat_HasPermission_ReturnsTrue() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoom.isParticipant(1L)).thenReturn(true);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));

        // When
        boolean result = chatService.canChat(1L, chatRoomId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("채팅 권한이 없으면 false 반환")
    void canChat_NoPermission_ReturnsFalse() {
        // Given
        Long chatRoomId = 1L;
        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.empty());

        // When
        boolean result = chatService.canChat(1L, chatRoomId);

        // Then
        assertThat(result).isFalse();
    }

    /**
     * getMessages 테스트
     */
    @Test
    @DisplayName("메시지 목록을 조회한다")
    void getMessages_Success() {
        // Given
        setupUserMocks();
        Long chatRoomId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        ChatRoom chatRoom = createMockChatRoom(chatRoomId, currentUser, adminUser);
        when(chatRoom.isParticipant(1L)).thenReturn(true);

        ChatMessage message = mock(ChatMessage.class);
        when(message.getId()).thenReturn(1L);
        when(message.getContent()).thenReturn("테스트 메시지");
        when(message.getSender()).thenReturn(currentUser);
        when(message.getChatRoom()).thenReturn(chatRoom);
        when(message.getCreatedAt()).thenReturn(LocalDateTime.now());

        Page<ChatMessage> messagePage = new PageImpl<>(List.of(message));

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByChatRoomId(chatRoomId, pageable)).thenReturn(messagePage);

        // When
        Page<ChatMessageResponse> responses = chatService.getMessages(chatRoomId, pageable, authUser);

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses.getContent()).hasSize(1);
        verify(chatMessageRepository).findByChatRoomId(chatRoomId, pageable);
    }

    /**
     * createBuyerSellerChatRoom 테스트
     */
    @Test
    @DisplayName("구매자-판매자 새 채팅방을 생성한다")
    void createBuyerSellerChatRoom_NewRoom_Success() {
        // Given
        setupUserMocks();
        Long dealId = 1L;

        User buyer = currentUser;
        User seller = adminUser;

        Deal deal = createMockDeal(dealId, buyer, seller);
        when(dealRepository.findByIdWithUsersForChat(dealId)).thenReturn(Optional.of(deal));
        when(chatRoomRepository.findByTwoUsers(1L, 2L)).thenReturn(Optional.empty());
        when(chatRoomRepository.findDeletedByTwoUsersAndDeal(1L, 2L, dealId)).thenReturn(Optional.empty());

        ChatRoom newChatRoom = mock(ChatRoom.class);
        when(newChatRoom.getId()).thenReturn(100L);
        when(newChatRoom.getOtherUser(1L)).thenReturn(seller);
        when(newChatRoom.getOtherUser(2L)).thenReturn(buyer);
        when(newChatRoom.getType()).thenReturn(ChatRoomType.BUYER_SELLER);
        when(newChatRoom.getUpdatedAt()).thenReturn(LocalDateTime.now());

        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(newChatRoom);
        when(chatMessageRepository.countUnreadMessages(any(), eq(1L))).thenReturn(0L);

        // When
        ChatRoomResponse response = chatService.createBuyerSellerChatRoom(dealId, authUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getChatRoomId()).isEqualTo(100L);
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(notificationService).sendNewChatRoomNotification(eq(2L), any(ChatRoomResponse.class));
    }

    @Test
    @DisplayName("구매자-판매자 기존 채팅방이 있으면 조회한다")
    void createBuyerSellerChatRoom_ExistingRoom_Success() {
        // Given
        setupUserMocks();
        Long dealId = 1L;

        User buyer = currentUser;
        User seller = adminUser;

        Deal deal = createMockDeal(dealId, buyer, seller);
        when(dealRepository.findByIdWithUsersForChat(dealId)).thenReturn(Optional.of(deal));

        ChatRoom existingRoom = mock(ChatRoom.class);
        when(existingRoom.getId()).thenReturn(100L);
        when(existingRoom.getOtherUser(1L)).thenReturn(seller);
        when(existingRoom.getType()).thenReturn(ChatRoomType.BUYER_SELLER);
        when(existingRoom.getUpdatedAt()).thenReturn(LocalDateTime.now());

        when(chatRoomRepository.findByTwoUsers(1L, 2L)).thenReturn(Optional.of(existingRoom));
        when(chatMessageRepository.countUnreadMessages(any(), eq(1L))).thenReturn(0L);

        // When
        ChatRoomResponse response = chatService.createBuyerSellerChatRoom(dealId, authUser);

        // Then
        assertThat(response).isNotNull();
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("구매자-판매자 삭제된 채팅방을 재활성화한다")
    void createBuyerSellerChatRoom_ReactivateDeletedRoom_Success() {
        // Given
        setupUserMocks();
        Long dealId = 1L;

        User buyer = currentUser;
        User seller = adminUser;

        Deal deal = createMockDeal(dealId, buyer, seller);
        when(dealRepository.findByIdWithUsersForChat(dealId)).thenReturn(Optional.of(deal));
        when(chatRoomRepository.findByTwoUsers(1L, 2L)).thenReturn(Optional.empty());

        ChatRoom deletedRoom = mock(ChatRoom.class);
        when(deletedRoom.getId()).thenReturn(100L);
        when(deletedRoom.getOtherUser(1L)).thenReturn(seller);
        when(deletedRoom.getType()).thenReturn(ChatRoomType.BUYER_SELLER);
        when(deletedRoom.getUpdatedAt()).thenReturn(LocalDateTime.now());

        when(chatRoomRepository.findDeletedByTwoUsersAndDeal(1L, 2L, dealId))
                .thenReturn(Optional.of(deletedRoom));
        when(chatMessageRepository.countUnreadMessages(any(), eq(1L))).thenReturn(0L);

        // When
        ChatRoomResponse response = chatService.createBuyerSellerChatRoom(dealId, authUser);

        // Then
        assertThat(response).isNotNull();
        verify(deletedRoom).reactivate();
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deal이 없으면 예외 발생")
    void createBuyerSellerChatRoom_DealNotFound_ThrowException() {
        // Given
        Long dealId = 999L;
        when(dealRepository.findByIdWithUsersForChat(dealId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.createBuyerSellerChatRoom(dealId, authUser));
    }

    @Test
    @DisplayName("구매자나 판매자가 아니면 예외 발생")
    void createBuyerSellerChatRoom_NotBuyerOrSeller_ThrowException() {
        // Given
        setupUserMocks();
        Long dealId = 1L;

        User buyer = mock(User.class);
        User seller = mock(User.class);
        when(buyer.getId()).thenReturn(10L);
        when(seller.getId()).thenReturn(20L);

        Deal deal = createMockDeal(dealId, buyer, seller);
        when(dealRepository.findByIdWithUsersForChat(dealId)).thenReturn(Optional.of(deal));

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.createBuyerSellerChatRoom(dealId, authUser));
    }

    @Test
    @DisplayName("경매 낙찰이 아니면 예외 발생")
    void createBuyerSellerChatRoom_NotSuccessfulBid_ThrowException() {
        // Given
        setupUserMocks();
        Long dealId = 1L;

        User buyer = currentUser;
        User seller = adminUser;

        Deal deal = createMockDealWithStatus(dealId, buyer, seller, AuctionStatusType.AUCTIONING);
        when(dealRepository.findByIdWithUsersForChat(dealId)).thenReturn(Optional.of(deal));

        // When & Then
        assertThrows(CustomException.class,
                () -> chatService.createBuyerSellerChatRoom(dealId, authUser));
    }

    /**
     * getAvailableUsers 테스트
     */
    @Test
    @DisplayName("일반 사용자가 채팅 가능한 관리자 목록을 조회한다")
    void getAvailableUsers_AsUser_ReturnsAdmins() {
        // Given
        setupUserMocks();

        when(chatRoomRepository.findByUserId(1L)).thenReturn(List.of());

        User admin1 = mock(User.class);
        when(admin1.getId()).thenReturn(10L);
        when(admin1.getName()).thenReturn("관리자1");
        when(admin1.getRole()).thenReturn(UserRole.ADMIN);

        when(userRepository.findByRoleIn(any())).thenReturn(List.of(admin1));
        when(dealRepository.findSuccessfulDealsByBuyerId(1L)).thenReturn(List.of());
        when(dealRepository.findSuccessfulDealsBySellerId(1L)).thenReturn(List.of());

        // When
        AvailableUsersResponse response = chatService.getAvailableUsers(authUser, false);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAdmins()).hasSize(1);
        assertThat(response.getDealUsers()).isEmpty();
    }

    @Test
    @DisplayName("구매자로서 채팅 가능한 판매자 목록을 조회한다")
    void getAvailableUsers_AsBuyer_ReturnsSellers() {
        // Given
        setupUserMocks();

        when(chatRoomRepository.findByUserId(1L)).thenReturn(List.of());
        when(userRepository.findByRoleIn(any())).thenReturn(List.of());

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(20L);
        when(seller.getName()).thenReturn("판매자");

        Deal deal = createMockDeal(1L, currentUser, seller);
        when(dealRepository.findSuccessfulDealsByBuyerId(1L)).thenReturn(List.of(deal));
        when(dealRepository.findSuccessfulDealsBySellerId(1L)).thenReturn(List.of());

        // When
        AvailableUsersResponse response = chatService.getAvailableUsers(authUser, false);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDealUsers()).hasSize(1);
    }

    @Test
    @DisplayName("판매자로서 채팅 가능한 구매자 목록을 조회한다")
    void getAvailableUsers_AsSeller_ReturnsBuyers() {
        // Given
        setupUserMocks();

        when(chatRoomRepository.findByUserId(1L)).thenReturn(List.of());
        when(userRepository.findByRoleIn(any())).thenReturn(List.of());

        User buyer = mock(User.class);
        when(buyer.getId()).thenReturn(30L);
        when(buyer.getName()).thenReturn("구매자");

        Deal deal = createMockDeal(1L, buyer, currentUser);
        when(dealRepository.findSuccessfulDealsByBuyerId(1L)).thenReturn(List.of());
        when(dealRepository.findSuccessfulDealsBySellerId(1L)).thenReturn(List.of(deal));

        // When
        AvailableUsersResponse response = chatService.getAvailableUsers(authUser, false);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDealUsers()).hasSize(1);
    }

    @Test
    @DisplayName("기존 채팅방 사용자를 제외하고 조회한다")
    void getAvailableUsers_ExcludeExistingUsers_Success() {
        // Given
        setupUserMocks();

        ChatRoom existingRoom = createMockChatRoom(1L, currentUser, adminUser);
        when(chatRoomRepository.findByUserId(1L)).thenReturn(List.of(existingRoom));

        when(userRepository.findByRoleIn(any())).thenReturn(List.of(adminUser));
        when(dealRepository.findSuccessfulDealsByBuyerId(1L)).thenReturn(List.of());
        when(dealRepository.findSuccessfulDealsBySellerId(1L)).thenReturn(List.of());

        // When
        AvailableUsersResponse response = chatService.getAvailableUsers(authUser, false);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAdmins()).isEmpty();  // 기존 채팅방 있는 admin 제외
    }

    @Test
    @DisplayName("기존 채팅방 사용자를 포함하여 조회한다")
    void getAvailableUsers_IncludeExistingUsers_Success() {
        // Given
        setupUserMocks();

        ChatRoom existingRoom = createMockChatRoom(1L, currentUser, adminUser);
        when(chatRoomRepository.findByUserId(1L)).thenReturn(List.of(existingRoom));

        when(userRepository.findByRoleIn(any())).thenReturn(List.of(adminUser));
        when(dealRepository.findSuccessfulDealsByBuyerId(1L)).thenReturn(List.of());
        when(dealRepository.findSuccessfulDealsBySellerId(1L)).thenReturn(List.of());

        // When
        AvailableUsersResponse response = chatService.getAvailableUsers(authUser, true);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAdmins()).hasSize(1);  // 기존 채팅방 있어도 포함
    }
}