package com.example.quicksells.domain.ask.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.quicksells.common.enums.AskType.*;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AskServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AskRepository askRepository;

    @InjectMocks
    private AskService askService;


    @Test
    @DisplayName("문의 생성 성공")
    void createAsk_success() {

        // given
        AuthUser authUser = new AuthUser(1L, "test@test.com", USER, "홍길동");

        User user = mock(User.class);

        AskCreateRequest request = new AskCreateRequest("AUCTION", "경매 문의 제목", "경매 문의 내용");

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(user));

        Ask ask = new Ask(user, AUCTION, "경매 문의 제목", "경매 문의 내용");

        when(askRepository.save(any(Ask.class))).thenReturn(ask);

        // when
        AskCreateResponse response = askService.createAsk(request, authUser);

        // then
        assertThat(response.getAskType()).isEqualTo(AUCTION);
        assertThat(response.getTitle()).isEqualTo("경매 문의 제목");
        assertThat(response.getContent()).isEqualTo("경매 문의 내용");

        verify(askRepository).save(any(Ask.class));
    }

    @Test
    @DisplayName("본인 문의 상세 조회 성공")
    void getOneAsk_success() {

        // given
        Long userId = 1L;
        AuthUser authUser = new AuthUser(userId, "test@test.com", USER, "홍길동");

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Ask ask = new Ask(user, AUCTION, "경매 문의 제목", "경매 문의 내용");;

        when(askRepository.findById(1L)).thenReturn(Optional.of(ask));

        // when
        AskGetResponse response = askService.getAsk(1L, authUser);

        // then
        assertThat(response.getTitle()).isEqualTo("경매 문의 제목");
    }

    @Test
    @DisplayName("본인 문의 상세 조회 실패 - 문의 작성자만 조회 가능함")
    void getOneAsk_fail_validateAskOwner() {

        // given
        Long askId = 1L;

        AuthUser authUser = new AuthUser(2L, "test@test.com", USER, "홍길동");

        Ask ask = mock(Ask.class);

        when(askRepository.findById(askId)).thenReturn(Optional.of(ask));
        when(ask.isWrittenBy(authUser.getId())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> askService.getAsk(askId, authUser))
                .isInstanceOf(CustomException.class)
                .hasMessage("본인의 문의 내역만 접근할 수 있습니다.");
    }

    @Test
    @DisplayName("문의 전체 조회 성공")
    void getAllAsks_success() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        User user = mock(User.class);
        when(user.getName()).thenReturn("홍길동");

        Ask ask = new Ask(user, AUCTION, "제목", "내용");

        Page<Ask> askPage = new PageImpl<>(List.of(ask), pageable, 1);

        when(askRepository.findAllAsks(pageable)).thenReturn(askPage);

        // when
        Page<AskGetAllResponse> response = askService.getAllAsks(pageable);

        // then
        assertThat(response).isNotEmpty();
        assertThat(response.getContent().get(0).getMaskedUserName()).isEqualTo("홍**");
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("문의 전체 조회 실패 - 조회된 문의 내역이 없음")
    void getAllAsks_fail_notFound() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        Page<Ask> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(askRepository.findAllAsks(pageable)).thenReturn(emptyPage);

        // when & then
        assertThatThrownBy(() -> askService.getAllAsks(pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage("문의 내역을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("문의 수정 성공 - 모든 필드 수정")
    void updateAsk_success() {

        // given
        Long askId = 1L;
        Long userId = 1L;
        AuthUser authUser = new AuthUser(userId, "test@test.com", USER, "홍길동");

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Ask ask = new Ask(user, ITEM, "기존 제목", "기존 내용");
        ReflectionTestUtils.setField(ask, "id", askId);

        when(askRepository.findById(askId)).thenReturn(Optional.of(ask));

        AskUpdateRequest request = new AskUpdateRequest("AUCTION", "수정된 제목", "수정된 내용");

        // when
        AskUpdateReponse response = askService.updateAsk(askId, request, authUser);

        // then
        assertThat(response.getAskId()).isEqualTo(askId);
        assertThat(response.getAskType()).isEqualTo(AUCTION);
        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(response.getContent()).isEqualTo("수정된 내용");

        assertThat(ask.getAskType()).isEqualTo(AUCTION);
        assertThat(ask.getTitle()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("문의 수정 성공 - 제목만 수정")
    void updateAsk_onlyTitle_success() {

        // given
        Long askId = 1L;
        Long userId = 1L;
        AuthUser authUser = new AuthUser(userId, "test@test.com", USER, "홍길동");

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Ask ask = new Ask(user, ITEM, "제목", "내용");
        when(askRepository.findById(askId)).thenReturn(Optional.of(ask));

        AskUpdateRequest request = new AskUpdateRequest("ITEM", "수정 제목", null);

        // when
        askService.updateAsk(askId, request, authUser);

        // then
        assertThat(ask.getTitle()).isEqualTo("수정 제목");
        assertThat(ask.getContent()).isEqualTo("내용");
    }

    @Test
    @DisplayName("문의 수정 실패 - 작성자가 아닌 유저가 수정 시도")
    void updateAsk_fail_forbidden() {

        // given
        Long askId = 1L;
        Long ownerId = 1L;
        Long strangerId = 2L;

        AuthUser authUser = new AuthUser(strangerId, "stranger@test.com", USER, "배추도사");

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);

        Ask ask = new Ask(owner, ETC, "제목", "내용");
        when(askRepository.findById(askId)).thenReturn(Optional.of(ask));

        AskUpdateRequest request = new AskUpdateRequest("ETC", "제목", "변경 내용");

        // when & then
        assertThatThrownBy(() -> askService.updateAsk(askId, request, authUser))
                .isInstanceOf(CustomException.class)
                .hasMessage("본인의 문의 내역만 접근할 수 있습니다.");
    }

    @Test
    @DisplayName("문의 수정 실패 - 수정하려는 문의가 존재하지 않음")
    void updateAsk_fail_notFound() {

        // given
        Long askId = 1L;
        AuthUser authUser = new AuthUser(1L, "test@test.com", USER, "홍길동");
        AskUpdateRequest request = new AskUpdateRequest("ETC", "제목", "내용");

        when(askRepository.findById(askId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> askService.updateAsk(askId, request, authUser))
                .isInstanceOf(CustomException.class)
                .hasMessage("문의 내역을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("본인 문의 삭제 성공")
    void deleteAsk_success() {

        // given
        Long userId = 1L;

        AuthUser authUser = new AuthUser(userId, "test@test.com", USER, "홍길동");

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Ask ask = new Ask(user, AUCTION, "제목", "내용");

        when(askRepository.findById(1L)).thenReturn(Optional.of(ask));

        // then
        askService.deleteAsk(1L, authUser);

        // then
        assertThat(ask.isDeleted()).isTrue();
    }
}