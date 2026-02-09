package com.example.quicksells.domain.chat.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.common.model.PageResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.chat.model.request.ChatMessageRequest;
import com.example.quicksells.domain.chat.model.request.ChatRoomCreateRequest;
import com.example.quicksells.domain.chat.model.response.AvailableUsersResponse;
import com.example.quicksells.domain.chat.model.response.ChatMessageResponse;
import com.example.quicksells.domain.chat.model.response.ChatRoomResponse;
import com.example.quicksells.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "채팅(chatting) 관리")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * USER_ADMIN 채팅방 생성 또는 조회
     * - 일반 사용자 ↔ 관리자/감정사
     * - 이미 존재하면 기존 채팅방 반환
     * - 일반 사용자끼리는 채팅 불가 (403 에러)
     */
    @Operation(summary = "사용자 - 관리자 채팅방 생성 또는 조회", description = "상대방과의 채팅방을 생성하거나 기존 채팅방을 반환합니다. " + "일반 사용자끼리는 채팅할 수 없습니다.")
    @PostMapping("/chat/rooms/user-admin")
    public ResponseEntity<CommonResponse> createOrGetChatRoom(@Valid @RequestBody ChatRoomCreateRequest request, @AuthenticationPrincipal AuthUser authUser) {

        ChatRoomResponse response = chatService.createOrGetChatRoom(request, authUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("채팅방 생성/조회에 성공했습니다.", response));
    }

    /**
     * BUYER_SELLER 채팅방 생성 (경매 낙찰 후)
     * - 구매자 ↔ 판매자
     * - 경매 낙찰 확인 필수:
     *   1. AppraiseStatus = AUCTION
     *   2. AuctionStatusType = SUCCESSFUL_BID
     * - 구매자 또는 판매자만 호출 가능
     * - 이미 존재하면 기존 채팅방 반환
     */
    @Operation(summary = "구매자-판매자 채팅방 생성 (경매 낙찰 후)", description = "경매 낙찰 후 구매자와 판매자가 채팅할 수 있는 채팅방을 생성합니다. " + "낙찰 전이거나 구매자/판매자가 아니면 403 에러가 발생합니다.")
    @PostMapping("/chat/rooms/buyer-seller/{dealId}")
    public ResponseEntity<CommonResponse> createBuyerSellerChatRoom(@PathVariable Long dealId, @AuthenticationPrincipal AuthUser authUser) {

        ChatRoomResponse response = chatService.createBuyerSellerChatRoom(dealId, authUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("채팅방 생성/조회에 성공했습니다.", response));
    }

    /**
     * 내 채팅방 목록 조회
     * - 최신 업데이트 순으로 정렬
     * - 마지막 메시지 포함
     * - 안 읽은 메시지 수 포함
     */
    @Operation(summary = "내 채팅방 목록 조회", description = "현재 사용자가 참여한 모든 채팅방을 조회합니다.")
    @GetMapping("/chat/rooms")
    public ResponseEntity<CommonResponse> getMyChatRooms(@AuthenticationPrincipal AuthUser authUser) {

        List<ChatRoomResponse> responses = chatService.getMyChatRooms(authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("내 채팅방 목록 조회에 성공했습니다.", responses));
    }

    /**
     * 채팅방 상세 조회
     */
    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방의 상세 정보를 조회합니다.")
    @GetMapping("/chat/rooms/{id}")
    public ResponseEntity<CommonResponse> getChatRoomDetail(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {

        ChatRoomResponse response = chatService.getChatRoomDetail(id, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("채팅방 상세 조회에 성공했습니다.", response));
    }

    /**
     * 메시지 전송
     * - chatRoomId는 URL 경로에서 받으므로 Request Body의 chatRoomId는 무시
     */
    @Operation(summary = "메시지 전송", description = "채팅방에 메시지를 전송합니다.")
    @PostMapping("/chat/rooms/{id}/messages")
    public ResponseEntity<CommonResponse> sendMessage(@PathVariable Long id, @Valid @RequestBody ChatMessageRequest request, @AuthenticationPrincipal AuthUser authUser) {

        // URL 경로의 id를 사용 (request.getChatRoomId()는 무시)
        ChatMessageResponse response = chatService.sendMessage(id, request, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("메시지 전송에 성공했습니다.", response));
    }

    /**
     * 메시지 목록 조회 (페이징)
     * - 최신 메시지부터 조회 (DESC)
     */
    @Operation(summary = "메시지 목록 조회", description = "채팅방의 메시지를 페이징하여 조회합니다. 최신 메시지부터 반환됩니다.")
    @GetMapping("/chat/rooms/{id}/messages")
    public ResponseEntity<PageResponse> getMessages(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal AuthUser authUser) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ChatMessageResponse> responses = chatService.getMessages(id, pageable, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("메시지 목록 조회에 성공했습니다.", responses));
    }

    /**
     * 채팅 가능한 사용자 목록 조회
     * - 관리자 목록 (항상 채팅 가능)
     * - 거래 상대방 목록 (경매 낙찰 후 채팅 가능)
     */
    @Operation(summary = "채팅 가능한 사용자 목록 조회", description = "새 채팅을 시작할 수 있는 사용자 목록을 반환합니다. " + "관리자와 경매 낙찰된 거래의 상대방이 포함됩니다.")
    @GetMapping("/chat/available-users")
    public ResponseEntity<CommonResponse> getAvailableUsers(@RequestParam(defaultValue = "false") boolean includeExisting, @AuthenticationPrincipal AuthUser authUser) {

        AvailableUsersResponse response = chatService.getAvailableUsers(authUser, includeExisting);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("채팅 가능한 사용자 목록 조회에 성공했습니다.", response));
    }

    /**
     * 메시지 읽음 처리
     * - 상대방이 보낸 모든 안 읽은 메시지를 읽음 처리
     */
    @Operation(summary = "메시지 읽음 처리", description = "채팅방의 상대방이 보낸 모든 안 읽은 메시지를 읽음 처리합니다.")
    @PutMapping("/chat/rooms/{id}/read")
    public ResponseEntity<CommonResponse> markMessagesAsRead(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {

        chatService.markMessagesAsRead(id, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("메시지를 읽음에 성공했습니다."));
    }

    /**
     * 채팅방 나가기
     */
    @Operation(summary = "채팅방 나가기", description = "채팅방에서 나갑니다. 양쪽 모두 나가면 완전히 삭제됩니다.")
    @DeleteMapping("/chat/rooms/{id}")
    public ResponseEntity<CommonResponse> leaveChatRoom(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {

        chatService.leaveChatRoom(id, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("채팅방 나가기에 성공했습니다."));
    }
}
