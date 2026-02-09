package com.example.quicksells.domain.answer.controller;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.common.exception.GlobalExceptionHandler;
import com.example.quicksells.domain.answer.model.request.AnswerCreateRequest;
import com.example.quicksells.domain.answer.model.request.AnswerUpdateRequest;
import com.example.quicksells.domain.answer.model.response.AnswerCreateResponse;
import com.example.quicksells.domain.answer.model.response.AnswerGetAllResponse;
import com.example.quicksells.domain.answer.model.response.AnswerGetResponse;
import com.example.quicksells.domain.answer.service.AnswerService;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.quicksells.common.enums.UserRole.ADMIN;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AnswerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnswerService answerService;

    @InjectMocks
    private AnswerController answerController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "admin@test.com", ADMIN, "관리자");

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(answerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
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
                })
                .build();
    }

    @Test
    @DisplayName("답변 생성 성공(관리자) - POST /api/answers/asks/{askId}")
    void createAnswer_success() throws Exception {
        Long askId = 10L;
        AnswerCreateRequest request = new AnswerCreateRequest("답변 제목", "답변 내용");

        AnswerCreateResponse response = new AnswerCreateResponse(
                100L, askId, 1L, "관리자", "답변 제목", "답변 내용", LocalDateTime.now()
        );

        when(answerService.createAnswer(eq(askId), any(AnswerCreateRequest.class), any(AuthUser.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/answers/asks/{askId}", askId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("답변 생성을 완료하였습니다"))
                .andExpect(jsonPath("$.data.answerId").value(100))
                .andExpect(jsonPath("$.data.askId").value(10))
                .andExpect(jsonPath("$.data.adminId").value(1))
                .andExpect(jsonPath("$.data.adminName").value("관리자"))
                .andExpect(jsonPath("$.data.title").value("답변 제목"))
                .andExpect(jsonPath("$.data.content").value("답변 내용"));
    }

    @Test
    @DisplayName("답변 상세 조회 성공 - GET /api/answers/asks/{askId}")
    void getAnswer_success() throws Exception {
        Long askId = 10L;

        AnswerGetResponse response = new AnswerGetResponse(
                100L, askId, "답변 제목", "답변 내용", "관리자", LocalDateTime.now()
        );

        when(answerService.getAnswer(eq(askId), any(AuthUser.class))).thenReturn(response);

        mockMvc.perform(get("/api/answers/asks/{askId}", askId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("답변 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.answerId").value(100))
                .andExpect(jsonPath("$.data.askId").value(10))
                .andExpect(jsonPath("$.data.title").value("답변 제목"))
                .andExpect(jsonPath("$.data.content").value("답변 내용"))
                .andExpect(jsonPath("$.data.adminName").value("관리자"));
    }

    @Test
    @DisplayName("답변 전체 조회 성공 - GET /api/answers")
    void getAnswers_success() throws Exception {
        List<AnswerGetAllResponse> response = List.of(
                new AnswerGetAllResponse(100L, 10L, "답변1", "관리자", LocalDateTime.now()),
                new AnswerGetAllResponse(101L, 11L, "답변2", "관리자", LocalDateTime.now())
        );

        when(answerService.getAnswers(any(AuthUser.class))).thenReturn(response);

        mockMvc.perform(get("/api/answers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("답변 전체조회를 성공하였습니다."))
                .andExpect(jsonPath("$.data[0].answerId").value(100))
                .andExpect(jsonPath("$.data[1].answerId").value(101));
    }

    @Test
    @DisplayName("답변 수정 성공 - PUT /api/admin/answers/{id}")
    void updateAnswer_success() throws Exception {
        Long answerId = 100L;
        AnswerUpdateRequest request = new AnswerUpdateRequest("수정 제목", "수정 내용");

        doNothing().when(answerService).updateAnswer(eq(answerId), any(AnswerUpdateRequest.class));

        mockMvc.perform(put("/api/admin/answers/{id}", answerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("답변 수정이 완료되었습니다."));
    }

    @Test
    @DisplayName("답변 삭제 성공 - DELETE /api/admin/answers/{id}")
    void deleteAnswer_success() throws Exception {
        Long answerId = 100L;

        doNothing().when(answerService).deleteAnswer(eq(answerId));

        mockMvc.perform(delete("/api/admin/answers/{id}", answerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("답변을 삭제하였습니다."));
    }

    @Test
    @DisplayName("답변 생성 실패 - @Valid(title/content NotBlank)로 400")
    void createAnswer_fail_validation() throws Exception {
        Long askId = 10L;

        // AnswerCreateRequest는 message 지정이 없어서 환경에 따라 달라질 수 있어 안정적으로 isNotEmpty()
        AnswerCreateRequest request = new AnswerCreateRequest("", "");

        mockMvc.perform(post("/api/answers/asks/{askId}", askId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("답변 수정 실패 - @Valid(message 지정)로 400")
    void updateAnswer_fail_validation() throws Exception {
        Long answerId = 100L;

        AnswerUpdateRequest request = new AnswerUpdateRequest("", "수정 내용");

        mockMvc.perform(put("/api/admin/answers/{id}", answerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                // AnswerUpdateRequest의 NotBlank message
                .andExpect(jsonPath("$.message").value("수정할 답변 제목을 입력해 주세요."));
    }

    @Test
    @DisplayName("답변 상세 조회 실패 - NOT_FOUND_ANSWER => 404 + message")
    void getAnswer_fail_notFoundAnswer() throws Exception {
        Long askId = 10L;

        when(answerService.getAnswer(eq(askId), any(AuthUser.class)))
                .thenThrow(new CustomException(ExceptionCode.NOT_FOUND_ANSWER));

        mockMvc.perform(get("/api/answers/asks/{askId}", askId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("답변 내역을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("답변 상세 조회 실패 - ACCESS_DENIED_ANSWER => 403 + message")
    void getAnswer_fail_accessDenied() throws Exception {
        Long askId = 10L;

        when(answerService.getAnswer(eq(askId), any(AuthUser.class)))
                .thenThrow(new CustomException(ExceptionCode.ACCESS_DENIED_ANSWER));

        mockMvc.perform(get("/api/answers/asks/{askId}", askId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("조회할 권한이 없습니다."));
    }

    @Test
    @DisplayName("답변 생성 실패 - INVALID_USER_ROLE => 400 + message")
    void createAnswer_fail_invalidRole() throws Exception {
        Long askId = 10L;
        AnswerCreateRequest request = new AnswerCreateRequest("t", "c");

        when(answerService.createAnswer(eq(askId), any(AnswerCreateRequest.class), any(AuthUser.class)))
                .thenThrow(new CustomException(ExceptionCode.INVALID_USER_ROLE));

        mockMvc.perform(post("/api/answers/asks/{askId}", askId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("유효하지 않은 사용자 권한입니다."));
    }

    @Test
    @DisplayName("답변 수정 실패 - NOT_FOUND_ANSWER => 404 + message")
    void updateAnswer_fail_notFoundAnswer() throws Exception {
        Long answerId = 999L;
        AnswerUpdateRequest request = new AnswerUpdateRequest("t", "c");

        doThrow(new CustomException(ExceptionCode.NOT_FOUND_ANSWER))
                .when(answerService).updateAnswer(eq(answerId), any(AnswerUpdateRequest.class));

        mockMvc.perform(put("/api/admin/answers/{id}", answerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("답변 내역을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("답변 삭제 실패 - NOT_FOUND_ANSWER => 404 + message")
    void deleteAnswer_fail_notFoundAnswer() throws Exception {
        Long answerId = 999L;

        doThrow(new CustomException(ExceptionCode.NOT_FOUND_ANSWER))
                .when(answerService).deleteAnswer(eq(answerId));

        mockMvc.perform(delete("/api/admin/answers/{id}", answerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("답변 내역을 찾을 수 없습니다."));
    }
}
