package com.example.quicksells.domain.chat.controller;

import com.example.quicksells.common.enums.ChatRoomType;
import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.chat.model.request.ChatMessageRequest;
import com.example.quicksells.domain.chat.model.request.ChatRoomCreateRequest;
import com.example.quicksells.domain.chat.model.response.AvailableUsersResponse;
import com.example.quicksells.domain.chat.model.response.ChatMessageResponse;
import com.example.quicksells.domain.chat.model.response.ChatRoomResponse;
import com.example.quicksells.domain.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @InjectMocks
    private ChatController chatController;

    @Mock
    private ChatService chatService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthUser authUser;
    private ChatRoomResponse chatRoomResponse;

    @BeforeEach
    void setUp() {

        // MockMvc 수동 설정
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setCustomArgumentResolvers(
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().equals(AuthUser.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return authUser;
                            }
                        },
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
        objectMapper = new ObjectMapper();

        authUser = new AuthUser(1L, "user@test.com", UserRole.USER, "테스트유저");

        // ChatRoomResponse Mock 데이터
        ChatRoomResponse.OtherUserDto otherUser = new ChatRoomResponse.OtherUserDto(2L, "관리자", "ADMIN");

        chatRoomResponse = new ChatRoomResponse(1L, ChatRoomType.USER_ADMIN, otherUser, "마지막 메시지", 0L, LocalDateTime.now());
    }

    @Test
    @DisplayName("사용자-관리자 채팅방 생성/조회 성공")
    void createOrGetChatRoom_Success() throws Exception {
        // Given
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(2L);

        when(chatService.createOrGetChatRoom(any(ChatRoomCreateRequest.class), any(AuthUser.class))).thenReturn(chatRoomResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/rooms/user-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅방 생성/조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.chatRoomId").value(1L))
                .andExpect(jsonPath("$.data.type").value("USER_ADMIN"));

        verify(chatService, times(1)).createOrGetChatRoom(any(ChatRoomCreateRequest.class), any(AuthUser.class));
    }

    @Test
    @DisplayName("구매자-판매자 채팅방 생성/조회 성공")
    void createBuyerSellerChatRoom_Success() throws Exception {
        // Given
        Long dealId = 1L;

        ChatRoomResponse.OtherUserDto seller = new ChatRoomResponse.OtherUserDto(3L, "판매자", "USER");

        ChatRoomResponse buyerSellerResponse = new ChatRoomResponse(2L, ChatRoomType.BUYER_SELLER, seller, null, 0L, LocalDateTime.now());

        when(chatService.createBuyerSellerChatRoom(eq(dealId), any(AuthUser.class))).thenReturn(buyerSellerResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/rooms/buyer-seller/{dealId}", dealId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅방 생성/조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.chatRoomId").value(2L))
                .andExpect(jsonPath("$.data.type").value("BUYER_SELLER"));

        verify(chatService, times(1)).createBuyerSellerChatRoom(eq(dealId), any(AuthUser.class));
    }

    @Test
    @DisplayName("내 채팅방 목록 조회 성공")
    void getMyChatRooms_Success() throws Exception {
        // Given
        List<ChatRoomResponse> responses = List.of(chatRoomResponse);

        when(chatService.getMyChatRooms(any(AuthUser.class))).thenReturn(responses);

        // When & Then
        mockMvc.perform(get("/api/chat/rooms"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 채팅방 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].chatRoomId").value(1L))
                .andExpect(jsonPath("$.data[0].type").value("USER_ADMIN"));

        verify(chatService, times(1)).getMyChatRooms(any(AuthUser.class));
    }

    @Test
    @DisplayName("내 채팅방 목록이 비어있을 때")
    void getMyChatRooms_EmptyList_Success() throws Exception {
        // Given
        when(chatService.getMyChatRooms(any(AuthUser.class))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/chat/rooms"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(chatService, times(1)).getMyChatRooms(any(AuthUser.class));
    }

    @Test
    @DisplayName("채팅방 상세 조회 성공")
    void getChatRoomDetail_Success() throws Exception {
        // Given
        Long chatRoomId = 1L;

        when(chatService.getChatRoomDetail(eq(chatRoomId), any(AuthUser.class))).thenReturn(chatRoomResponse);

        // When & Then
        mockMvc.perform(get("/api/chat/rooms/{id}", chatRoomId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅방 상세 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.chatRoomId").value(1L))
                .andExpect(jsonPath("$.data.otherUser.userId").value(2L));

        verify(chatService, times(1)).getChatRoomDetail(eq(chatRoomId), any(AuthUser.class));
    }

    @Test
    @DisplayName("메시지 전송 성공")
    void sendMessage_Success() throws Exception {
        // Given
        Long chatRoomId = 1L;

        // ObjectMapper 대신 직접 JSON 문자열 작성
        String requestJson = """
        {
            "content": "안녕하세요"
        }
        """;

        ChatMessageResponse messageResponse = new ChatMessageResponse(1L, chatRoomId, 1L, "테스트유저", "안녕하세요", false, LocalDateTime.now());

        when(chatService.sendMessage(eq(chatRoomId), any(ChatMessageRequest.class), any(AuthUser.class))).thenReturn(messageResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/rooms/{id}/messages", chatRoomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))  // 직접 작성한 JSON 사용
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("메시지 전송에 성공했습니다."))
                .andExpect(jsonPath("$.data.content").value("안녕하세요"));

        verify(chatService, times(1)).sendMessage(eq(chatRoomId), any(ChatMessageRequest.class), any(AuthUser.class));
    }


    @Test
    @DisplayName("메시지 목록 조회 - 기본 페이징 파라미터")
    void getMessages_DefaultPagination_Success() throws Exception {
        // Given
        Long chatRoomId = 1L;
        Page<ChatMessageResponse> emptyPage = new PageImpl<>(List.of());

        when(chatService.getMessages(eq(chatRoomId), any(Pageable.class), any(AuthUser.class))).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/chat/rooms/{id}/messages", chatRoomId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());

        verify(chatService, times(1)).getMessages(eq(chatRoomId), any(Pageable.class), any(AuthUser.class));
    }

    @Test
    @DisplayName("채팅 가능한 사용자 목록 조회 성공")
    void getAvailableUsers_Success() throws Exception {
        // Given
        AvailableUsersResponse.UserSummary admin = new AvailableUsersResponse.UserSummary(2L, "관리자", "ADMIN", false);

        AvailableUsersResponse availableUsersResponse = new AvailableUsersResponse(List.of(admin), List.of());

        when(chatService.getAvailableUsers(any(AuthUser.class), eq(false))).thenReturn(availableUsersResponse);

        // When & Then
        mockMvc.perform(get("/api/chat/available-users")
                        .param("includeExisting", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅 가능한 사용자 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.admins").isArray())
                .andExpect(jsonPath("$.data.admins[0].userId").value(2L))
                .andExpect(jsonPath("$.data.admins[0].name").value("관리자"))
                .andExpect(jsonPath("$.data.dealUsers").isArray());

        verify(chatService, times(1)).getAvailableUsers(any(AuthUser.class), eq(false));
    }

    @Test
    @DisplayName("채팅 가능한 사용자 목록 조회 - 기존 사용자 포함")
    void getAvailableUsers_IncludeExisting_Success() throws Exception {
        // Given
        AvailableUsersResponse.UserSummary admin = new AvailableUsersResponse.UserSummary(2L, "관리자", "ADMIN", true);

        AvailableUsersResponse availableUsersResponse = new AvailableUsersResponse(List.of(admin), List.of());

        when(chatService.getAvailableUsers(any(AuthUser.class), eq(true))).thenReturn(availableUsersResponse);

        // When & Then
        mockMvc.perform(get("/api/chat/available-users")
                        .param("includeExisting", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.admins[0].hasExistingChat").value(true));

        verify(chatService, times(1)).getAvailableUsers(any(AuthUser.class), eq(true));
    }

    @Test
    @DisplayName("메시지 읽음 처리 성공")
    void markMessagesAsRead_Success() throws Exception {
        // Given
        Long chatRoomId = 1L;

        doNothing().when(chatService).markMessagesAsRead(eq(chatRoomId), any(AuthUser.class));

        // When & Then
        mockMvc.perform(put("/api/chat/rooms/{id}/read", chatRoomId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("메시지를 읽음에 성공했습니다."));

        verify(chatService, times(1)).markMessagesAsRead(eq(chatRoomId), any(AuthUser.class));
    }

    @Test
    @DisplayName("채팅방 나가기 성공")
    void leaveChatRoom_Success() throws Exception {
        // Given
        Long chatRoomId = 1L;

        doNothing().when(chatService).leaveChatRoom(eq(chatRoomId), any(AuthUser.class));

        // When & Then
        mockMvc.perform(delete("/api/chat/rooms/{id}", chatRoomId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅방 나가기에 성공했습니다."));

        verify(chatService, times(1)).leaveChatRoom(eq(chatRoomId), any(AuthUser.class));
    }
}