package com.example.quicksells.domain.chat.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅 메시지 전송 요청")
public class ChatMessageRequest {

    /**
     * REST API에서는 URL 경로(/api/chat/rooms/{id}/messages)로 chatRoomId를 받지만,
     * WebSocket에서는 메시지 본문에 포함해야 한다.
     */
    @Schema(description = "채팅방 ID (WebSocket에서 사용)", example = "1")
    private Long chatRoomId;  // WebSocket용 추가

    @NotBlank(message = "메시지 내용은 필수입니다")
    @Schema(description = "메시지 내용", example = "안녕하세요")
    private String content;
}
