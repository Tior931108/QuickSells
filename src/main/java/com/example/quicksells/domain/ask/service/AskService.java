package com.example.quicksells.domain.ask.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.ask.entity.Ask;
import com.example.quicksells.domain.ask.model.request.AskCreateRequest;
import com.example.quicksells.domain.ask.model.request.AskUpdateRequest;
import com.example.quicksells.domain.ask.model.response.AskCreateResponse;
import com.example.quicksells.domain.ask.model.response.AskGetAllResponse;
import com.example.quicksells.domain.ask.model.response.AskGetResponse;
import com.example.quicksells.domain.ask.model.response.AskUpdateReponse;
import com.example.quicksells.domain.ask.repository.AskRepository;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AskService {

    private final AskRepository askRepository;
    private final UserRepository userRepository;

    /**
     * 문의 생성
     */
    @Transactional
    public AskCreateResponse createAsk(AskCreateRequest request, AuthUser authUser) {

        // 1. 유저 조회
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        // 2. 문의 저장
        Ask ask = new Ask(user, request.getAskType(), request.getTitle(), request.getContent());

        Ask savedAsk = askRepository.save(ask);

        // 3. DTO 반환
        return AskCreateResponse.from(savedAsk);
    }

    /**
     * 문의 전체 조회 (페이징)
     * - 제목, 마스킹된 유저 이름, 생성 시점만 표시
     */
    @Transactional(readOnly = true)
    public Page<AskGetAllResponse> getAllAsks(Pageable pageable) {

        // 1. 문의 전체 조회 페이징
        Page<Ask> askPage = askRepository.findAllAsks(pageable);

        // 2. 페이징 DB 조회시 문의 내역이 하나도 없다면 예외처리
        if (!askPage.hasContent()) {
            throw new CustomException(ExceptionCode.NOT_FOUND_ASK);
        }

        // 3. 페이지 반환
        return askPage.map(AskGetAllResponse::from);
    }

    /**
     * 문의 상세 조회 (본인만 가능)
     */
    @Transactional(readOnly = true)
    public AskGetResponse getAsk(Long askId, AuthUser authUser) {

        // 1. 문의 조회
        Ask ask = getAskVerify(askId);

        // 2. 권한 확인: 본인만 조회 가능
        validateAskOwner(ask, authUser.getId());

        return AskGetResponse.from(ask);
    }

    /**
     * 문의 수정 (본인만 가능)
     */
    @Transactional
    public AskUpdateReponse updateAsk(Long askId, AskUpdateRequest request, AuthUser authUser) {
        // 1. 문의 조회
        Ask ask = getAskVerify(askId);

        // 2. 권한 확인: 본인만 수정 가능
        validateAskOwner(ask, authUser.getId());

        // 3. 부분 수정 (Optional)
        ask.updatePartial(request);

        return AskUpdateReponse.from(ask);
    }

    /**
     * 문의 삭제 (본인만 가능)
     */
    @Transactional
    public void deleteAsk(Long askId, AuthUser authUser) {
        // 1. 문의 조회
        Ask ask = getAskVerify(askId);

        // 2. 권한 확인: 본인만 삭제 가능
        validateAskOwner(ask, authUser.getId());

        // 3. Soft Delete
        ask.delete();
    }

    /**
     * 문의 내역 검증
     */
    private Ask getAskVerify(Long askId) {

        return askRepository.findById(askId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ASK));
    }

    /**
     * 문의 작성자 검증
     */
    private void validateAskOwner(Ask ask, Long userId) {

        if (!ask.isWrittenBy(userId)) {
            throw new CustomException(ExceptionCode.ONLY_OWNER_ASK);
        }
    }


}
