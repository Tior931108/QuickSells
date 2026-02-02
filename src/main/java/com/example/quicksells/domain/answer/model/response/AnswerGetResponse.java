package com.example.quicksells.domain.answer.model.response;

import com.example.quicksells.domain.answer.entity.Answer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class AnswerGetResponse {

    private final Long answerId;
    private final Long askId;
    private final String title;
    private final String content;
    private final String adminName;
    private final LocalDateTime createdAt;

    public static AnswerGetResponse from(Answer answer) {
        return new AnswerGetResponse(
                answer.getId(),
                answer.getAsk().getId(),
                answer.getTitle(),
                answer.getContent(),
                answer.getAdmin().getName(),
                answer.getCreatedAt()
        );
    }
}
