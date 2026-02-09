package com.example.quicksells.domain.answer.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.answer.entity.Answer;
import com.example.quicksells.domain.answer.model.request.AnswerCreateRequest;
import com.example.quicksells.domain.answer.model.request.AnswerUpdateRequest;
import com.example.quicksells.domain.answer.model.response.AnswerCreateResponse;
import com.example.quicksells.domain.answer.model.response.AnswerGetAllResponse;
import com.example.quicksells.domain.answer.model.response.AnswerGetResponse;
import com.example.quicksells.domain.answer.repository.AnswerRepository;
import com.example.quicksells.domain.ask.entity.Ask;
import com.example.quicksells.domain.ask.repository.AskRepository;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final AskRepository askRepository;
    private final UserRepository userRepository;

    /**
     * 답변 생성(관리자)
     */
    @Transactional
    public AnswerCreateResponse createAnswer(Long askId, AnswerCreateRequest request, AuthUser authUser) {

        // 관리자 검증
        if (authUser.getRole() != UserRole.ADMIN) {
            throw new CustomException(ExceptionCode.INVALID_USER_ROLE);
        }

        // 질문 찾기
        Ask ask = askRepository.findById(askId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ASK));

        // 답변 존재여부
        if (answerRepository.existsByAsk(ask)) {
            throw new CustomException(ExceptionCode.ANSWER_ALREADY_EXISTS);
        }

        // 관리자 유저 존재여부
        User admin = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        Answer answer = new Answer(ask, admin, request.getTitle(), request.getContent());

        // 답변 저장
        Answer savedAnswer = answerRepository.save(answer);

        return AnswerCreateResponse.from(savedAnswer);
    }

    /**
     * 답변 조회(유저/관리자)
     */
    @Transactional(readOnly = true)
    public AnswerGetResponse getAnswer(Long askId, AuthUser authUser) {

        AnswerGetResponse response = answerRepository.findByAskId(askId)
                        .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ANSWER));

        // ADMIN은 통과
        if (authUser.getRole() == UserRole.ADMIN) {
            return response;
        }

        // USER는 본인 Ask만
        Ask ask = askRepository.findById(askId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ASK));

        if (!ask.getUser().getId().equals(authUser.getId())) {
            throw new CustomException(ExceptionCode.ACCESS_DENIED_ANSWER);
        }

        return response;
    }

    /**
     * 답변 전체 조회
     */
    @Transactional(readOnly = true)
    public List<AnswerGetAllResponse> getAnswers(AuthUser authUser) {

        if (authUser.getRole() == UserRole.ADMIN) {
            return answerRepository.findAllByAdmin();
        }

        return answerRepository.findAllByUser(authUser.getId());
    }

    /**
     * 답변 수정
     */
    @Transactional
    public void updateAnswer(Long answerId, AnswerUpdateRequest request) {

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ANSWER));

        answer.update(request.getTitle(), request.getContent());
    }

    /**
     * 답변 삭제
     */
    @Transactional
    public void deleteAnswer(Long answerId) {

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ANSWER));

        answer.delete();
    }

}
