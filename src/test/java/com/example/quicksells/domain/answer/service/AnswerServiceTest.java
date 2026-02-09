package com.example.quicksells.domain.answer.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static com.example.quicksells.common.enums.AskType.ETC;
import static com.example.quicksells.common.enums.UserRole.ADMIN;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AskRepository askRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnswerService answerService;

    @Test
    @DisplayName("답변 생성 성공(관리자)")
    void createAnswer_success() {
        // given
        Long askId = 10L;
        AuthUser adminAuth = new AuthUser(1L, "admin@test.com", ADMIN, "관리자");
        AnswerCreateRequest request = new AnswerCreateRequest("답변 제목", "답변 내용");

        User asker = new User("user@test.com", "Password!23", "김유저", "010-1234-1234", "서울", "20010101");
        ReflectionTestUtils.setField(asker, "id", 2L);

        Ask ask = new Ask(asker, ETC, "문의 제목", "문의 내용");
        ReflectionTestUtils.setField(ask, "id", askId);

        User admin = new User("admin@test.com", "Password!23", "관리자", "010-2345-2345", "서울", "20010101");
        ReflectionTestUtils.setField(admin, "id", 1L);

        when(askRepository.findById(askId)).thenReturn(Optional.of(ask));
        when(answerRepository.existsByAsk(ask)).thenReturn(false);
        when(userRepository.findById(adminAuth.getId())).thenReturn(Optional.of(admin));

        Answer saved = new Answer(ask, admin, request.getTitle(), request.getContent());
        ReflectionTestUtils.setField(saved, "id", 100L);
        ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());

        when(answerRepository.save(any(Answer.class))).thenReturn(saved);

        // when
        AnswerCreateResponse response = answerService.createAnswer(askId, request, adminAuth);

        // then
        assertThat(response.getAnswerId()).isEqualTo(100L);
        assertThat(response.getAskId()).isEqualTo(10L);
        assertThat(response.getAdminId()).isEqualTo(1L);
        assertThat(response.getAdminName()).isEqualTo("관리자");
        assertThat(response.getTitle()).isEqualTo("답변 제목");
        assertThat(response.getContent()).isEqualTo("답변 내용");

        verify(answerRepository).save(any(Answer.class));
    }

    @Test
    @DisplayName("답변 생성 실패 - 관리자가 아님")
    void createAnswer_fail_notAdmin() {
        // given
        AuthUser userAuth = new AuthUser(2L, "user@test.com", USER, "김유저");
        AnswerCreateRequest request = new AnswerCreateRequest("답변 제목", "답변 내용");

        // when & then
        assertThatThrownBy(() -> answerService.createAnswer(10L, request, userAuth))
                .isInstanceOf(CustomException.class)
                .hasMessage("유효하지 않은 사용자 권한입니다.");
    }

    @Test
    @DisplayName("답변 조회 성공 - ADMIN은 권한 검사 없이 조회")
    void getAnswer_success_admin() {
        // given
        Long askId = 10L;
        AuthUser adminAuth = new AuthUser(1L, "admin@test.com", ADMIN, "관리자");

        AnswerGetResponse repoResponse = new AnswerGetResponse(
                100L, askId, "답변 제목", "답변 내용", "관리자", LocalDateTime.now()
        );

        when(answerRepository.findByAskId(askId)).thenReturn(Optional.of(repoResponse));

        // when
        AnswerGetResponse response = answerService.getAnswer(askId, adminAuth);

        // then
        assertThat(response.getAnswerId()).isEqualTo(100L);
        verify(askRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("답변 조회 성공 - USER가 본인 문의(Ask)인 경우")
    void getAnswer_success_user_ownAsk() {
        // given
        Long askId = 10L;
        Long userId = 2L;
        AuthUser userAuth = new AuthUser(userId, "user@test.com", USER, "김유저");

        AnswerGetResponse repoResponse = new AnswerGetResponse(
                100L, askId, "답변 제목", "답변 내용", "관리자", LocalDateTime.now()
        );
        when(answerRepository.findByAskId(askId)).thenReturn(Optional.of(repoResponse));

        User asker = new User("user@test.com", "Password!23", "김유저", "010-1234-1234", "서울", "20010101");
        ReflectionTestUtils.setField(asker, "id", userId);

        Ask ask = new Ask(asker, ETC, "문의 제목", "문의 내용");
        ReflectionTestUtils.setField(ask, "id", askId);

        when(askRepository.findById(askId)).thenReturn(Optional.of(ask));

        // when
        AnswerGetResponse response = answerService.getAnswer(askId, userAuth);

        // then
        assertThat(response.getAnswerId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("답변 조회 실패 - USER가 타인의 문의(Ask) 조회 시도")
    void getAnswer_fail_user_accessDenied() {
        // given
        Long askId = 10L;
        AuthUser userAuth = new AuthUser(2L, "user@test.com", USER, "김유저");

        AnswerGetResponse repoResponse = new AnswerGetResponse(
                100L, askId, "답변 제목", "답변 내용", "관리자", LocalDateTime.now()
        );
        when(answerRepository.findByAskId(askId)).thenReturn(Optional.of(repoResponse));

        User other = new User("other@test.com", "Password!23", "다른유저", "010-2345-2345", "서울", "20010101");
        ReflectionTestUtils.setField(other, "id", 999L);

        Ask ask = new Ask(other, ETC, "문의 제목", "문의 내용");
        ReflectionTestUtils.setField(ask, "id", askId);

        when(askRepository.findById(askId)).thenReturn(Optional.of(ask));

        // when & then
        assertThatThrownBy(() -> answerService.getAnswer(askId, userAuth))
                .isInstanceOf(CustomException.class)
                .hasMessage("조회할 권한이 없습니다.");
    }

    @Test
    @DisplayName("답변 전체 조회 - ADMIN")
    void getAnswers_admin() {
        // given
        AuthUser adminAuth = new AuthUser(1L, "admin@test.com", ADMIN, "관리자");

        when(answerRepository.findAllByAdmin()).thenReturn(List.of(
                new AnswerGetAllResponse(100L, 10L, "답변1", "관리자", LocalDateTime.now())
        ));

        // when
        List<AnswerGetAllResponse> responses = answerService.getAnswers(adminAuth);

        // then
        assertThat(responses).hasSize(1);
        verify(answerRepository).findAllByAdmin();
        verify(answerRepository, never()).findAllByUser(anyLong());
    }

    @Test
    @DisplayName("답변 전체 조회 - USER")
    void getAnswers_user() {
        // given
        Long userId = 2L;
        AuthUser userAuth = new AuthUser(userId, "user@test.com", USER, "김유저");

        when(answerRepository.findAllByUser(userId)).thenReturn(List.of(
                new AnswerGetAllResponse(100L, 10L, "답변1", "관리자", LocalDateTime.now())
        ));

        // when
        List<AnswerGetAllResponse> responses = answerService.getAnswers(userAuth);

        // then
        assertThat(responses).hasSize(1);
        verify(answerRepository).findAllByUser(userId);
        verify(answerRepository, never()).findAllByAdmin();
    }

    @Test
    @DisplayName("답변 수정 성공")
    void updateAnswer_success() {
        // given
        Long answerId = 100L;
        AnswerUpdateRequest request = new AnswerUpdateRequest("수정 제목", "수정 내용");

        User admin = new User("admin@test.com", "Password!23", "관리자", "010-1234-1234", "서울", "20010101");
        ReflectionTestUtils.setField(admin, "id", 1L);

        User asker = new User("user@test.com", "Password!23", "김유저", "010-2345-2345", "서울", "20010101");
        ReflectionTestUtils.setField(asker, "id", 2L);

        Ask ask = new Ask(asker, ETC, "문의 제목", "문의 내용");
        ReflectionTestUtils.setField(ask, "id", 10L);

        Answer answer = new Answer(ask, admin, "기존 제목", "기존 내용");
        ReflectionTestUtils.setField(answer, "id", answerId);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        // when
        answerService.updateAnswer(answerId, request);

        // then
        assertThat(answer.getTitle()).isEqualTo("수정 제목");
        assertThat(answer.getContent()).isEqualTo("수정 내용");
    }

    @Test
    @DisplayName("답변 삭제 성공")
    void deleteAnswer_success() {
        // given
        Long answerId = 100L;

        User admin = new User("admin@test.com", "Password!23", "관리자", "010-1234-1234", "서울", "20010101");
        ReflectionTestUtils.setField(admin, "id", 1L);

        User asker = new User("user@test.com", "Password!23", "김유저", "010-2345-2345", "서울", "20010101");
        ReflectionTestUtils.setField(asker, "id", 2L);

        Ask ask = new Ask(asker, ETC, "문의 제목", "문의 내용");
        ReflectionTestUtils.setField(ask, "id", 10L);

        Answer answer = new Answer(ask, admin, "제목", "내용");
        ReflectionTestUtils.setField(answer, "id", answerId);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        // when
        answerService.deleteAnswer(answerId);

        // then
        assertThat(answer.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("답변 생성 실패 - 문의(Ask) 없음")
    void createAnswer_fail_askNotFound() {
        Long askId = 999L;
        AuthUser adminAuth = new AuthUser(1L, "admin@test.com", ADMIN, "관리자");
        AnswerCreateRequest request = new AnswerCreateRequest("t", "c");

        when(askRepository.findById(askId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.createAnswer(askId, request, adminAuth))
                .isInstanceOf(CustomException.class)
                .hasMessage("문의 내역을 찾을 수 없습니다."); // ExceptionCode 메시지에 맞춰서 조정
    }

    @Test
    @DisplayName("답변 생성 실패 - 이미 답변이 존재")
    void createAnswer_fail_alreadyAnswered() {
        Long askId = 10L;
        AuthUser adminAuth = new AuthUser(1L, "admin@test.com", ADMIN, "관리자");
        AnswerCreateRequest request = new AnswerCreateRequest("t", "c");

        Ask ask = mock(Ask.class);

        when(askRepository.findById(askId)).thenReturn(Optional.of(ask));
        when(answerRepository.existsByAsk(ask)).thenReturn(true);

        assertThatThrownBy(() -> answerService.createAnswer(askId, request, adminAuth))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 답변이 존재합니다.");
    }

    @Test
    @DisplayName("답변 생성 실패 - 관리자 유저 없음")
    void createAnswer_fail_adminUserNotFound() {
        Long askId = 10L;
        AuthUser adminAuth = new AuthUser(1L, "admin@test.com", ADMIN, "관리자");
        AnswerCreateRequest request = new AnswerCreateRequest("t", "c");

        Ask ask = mock(Ask.class);

        when(askRepository.findById(askId)).thenReturn(Optional.of(ask));
        when(answerRepository.existsByAsk(ask)).thenReturn(false);
        when(userRepository.findById(adminAuth.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.createAnswer(askId, request, adminAuth))
                .isInstanceOf(CustomException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }
}
