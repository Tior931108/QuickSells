package com.example.quicksells.domain.answer.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.domain.answer.model.request.AnswerCreateRequest;
import com.example.quicksells.domain.answer.model.request.AnswerUpdateRequest;
import com.example.quicksells.domain.answer.model.response.AnswerCreateResponse;
import com.example.quicksells.domain.answer.model.response.AnswerGetAllResponse;
import com.example.quicksells.domain.answer.model.response.AnswerGetResponse;
import com.example.quicksells.domain.answer.service.AnswerService;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "문의 답변(answer) 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AnswerController {

    private final AnswerService answerService;

    /**
     * 답변 생성 API (관리자)
     */
    @Operation(summary = "문의 답변 생성")
    @PostMapping("/answers/asks/{askId}")
    public ResponseEntity<CommonResponse> createAnswer(@PathVariable Long askId, @Valid @RequestBody AnswerCreateRequest request, @AuthenticationPrincipal AuthUser authUser) {

        AnswerCreateResponse response = answerService.createAnswer(askId, request, authUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("답변 생성을 완료하였습니다", response));
    }

    /**
     * 답변 상세 조회
     */
    @Operation(summary = "문의 답변 상세 조회")
    @GetMapping("/answers/asks/{askId}")
    public ResponseEntity<CommonResponse> getAnswer(@PathVariable Long askId, @AuthenticationPrincipal AuthUser authUser) {

        AnswerGetResponse response = answerService.getAnswer(askId, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("답변 조회에 성공하였습니다.", response));
    }

    /**
     * 답변 전체 조회
     */
    @Operation(summary = "문의 답변 전체 조회")
    @GetMapping("/answers")
    public ResponseEntity<CommonResponse> getAnswers(@AuthenticationPrincipal AuthUser authUser) {

        List<AnswerGetAllResponse> response = answerService.getAnswers(authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("답변 전체조회를 성공하였습니다.", response));
    }

    /**
     * 답변 수정
     */
    @Operation(summary = "문의 답변 수정")
    @PutMapping("/admin/answers/{id}")
    public ResponseEntity<CommonResponse> updateAnswer(@PathVariable Long id, @Valid @RequestBody AnswerUpdateRequest request) {

        answerService.updateAnswer(id, request);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("답변 수정이 완료되었습니다."));
    }

    /**
     * 답변 삭제
     */
    @Operation(summary = "문의 답변 삭제")
    @DeleteMapping("/admin/answers/{id}")
    public ResponseEntity<CommonResponse> deleteAnswer(@PathVariable Long id) {

        answerService.deleteAnswer(id);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("답변을 삭제하였습니다."));
    }
}
