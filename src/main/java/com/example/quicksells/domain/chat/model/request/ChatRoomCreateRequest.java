package com.example.quicksells.domain.chat.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅방 생성 요청")
public class ChatRoomCreateRequest {

    @NotNull(message = "상대방 ID는 필수입니다")
    @Schema(description = "상대방 사용자 ID", example = "2")
    private Long otherUserId;
}
