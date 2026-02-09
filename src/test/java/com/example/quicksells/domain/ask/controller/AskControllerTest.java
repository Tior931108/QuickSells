package com.example.quicksells.domain.ask.controller;

import com.example.quicksells.domain.ask.model.request.AskCreateRequest;
import com.example.quicksells.domain.ask.model.request.AskUpdateRequest;
import com.example.quicksells.domain.ask.model.response.AskCreateResponse;
import com.example.quicksells.domain.ask.model.response.AskGetAllResponse;
import com.example.quicksells.domain.ask.model.response.AskGetResponse;
import com.example.quicksells.domain.ask.model.response.AskUpdateReponse;
import com.example.quicksells.domain.ask.service.AskService;
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
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.quicksells.common.enums.AskType.AUCTION;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AskService askService;

    @InjectMocks
    private AskController askController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "test@test.com", USER, "홍길동");

        mockMvc = MockMvcBuilders.standaloneSetup(askController)
                .setCustomArgumentResolvers(
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().equals(AuthUser.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return authUser;
                            }
                        },

                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("문의 생성 성공")
    void createAsk_success() throws Exception {

        // given
        AskCreateRequest request = new AskCreateRequest("AUCTION", "제목", "내용");
        AskCreateResponse response = new AskCreateResponse(1L, 1L, "홍길동", AUCTION, "제목", "내용", LocalDateTime.now(), LocalDateTime.now());

        when(askService.createAsk(any(AskCreateRequest.class), any(AuthUser.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/asks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("문의 생성에 성공했습니다."))
                .andExpect(jsonPath("$.data.askId").value(1L))
                .andExpect(jsonPath("$.data.userName").value("홍길동"))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.content").value("내용"));
    }

    @Test
    @DisplayName("본인 문의 상세 조회 성공")
    void getOneAsk_success() throws Exception {

        // given
        Long askId = 1L;
        AskGetResponse response = new AskGetResponse(askId, 1L, "홍길동", AUCTION, "제목", "내용", LocalDateTime.now());

        when(askService.getAsk(any(), any(AuthUser.class))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/asks/{id}", askId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("문의 상세 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.askId").value(1L))
                .andExpect(jsonPath("$.data.userName").value("홍길동"))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.content").value("내용"));
    }

    @Test
    @DisplayName("문의 전체 조회 성공")
    void getAllAsks_success() throws Exception {

        // given
        AskGetAllResponse ask1 = new AskGetAllResponse(1L, "제목1", "홍**", LocalDateTime.now());
        AskGetAllResponse ask2 = new AskGetAllResponse(2L, "제목2", "최**", LocalDateTime.now());
        List<AskGetAllResponse> askList = List.of(ask1, ask2);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AskGetAllResponse> pageResponse = new PageImpl<>(askList, pageable, askList.size());

        when(askService.getAllAsks(any(Pageable.class))).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/asks")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("문의 전체 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.content[0].title").value("제목1"))
                .andExpect(jsonPath("$.data.content[1].title").value("제목2"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }


    @Test
    @DisplayName("문의 전체 조회 - 오름차순 정렬")
    void getAllAsks_asc_success() throws Exception {

        // given
        List<AskGetAllResponse> askList = List.of(new AskGetAllResponse(1L, "제목1", "홍**", LocalDateTime.now()));
        Page<AskGetAllResponse> responsePage = new PageImpl<>(askList);

        when(askService.getAllAsks(any(Pageable.class))).thenReturn(responsePage);

        // when & then
        mockMvc.perform(get("/api/asks")
                        .param("page", "0")
                        .param("size", "10")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("문의 전체 조회에 성공했습니다."));
    }

    @Test
    @DisplayName("문의 수정 성공")
    void updateAsk_success() throws Exception {

        // given
        Long askId = 1L;

        AskUpdateRequest request = new AskUpdateRequest("AUCTION", "수정 제목", "수정 내용");
        AskUpdateReponse response = new AskUpdateReponse(askId, AUCTION, "수정 제목", "수정 내용", LocalDateTime.now());

        when(askService.updateAsk(any(), any(AskUpdateRequest.class), any(AuthUser.class))).thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/asks/{id}", askId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("문의 수정에 성공했습니다."))
                .andExpect(jsonPath("$.data.askId").value(1L))
                .andExpect(jsonPath("$.data.title").value("수정 제목"))
                .andExpect(jsonPath("$.data.content").value("수정 내용"));
    }

    @Test
    @DisplayName("문의 삭제 성공")
    void deleteAsk_success() throws Exception {

        // given
        Long askId = 1L;

        // when
        askService.deleteAsk(any(), any(AuthUser.class));

        // then
        mockMvc.perform(delete("/api/asks/{id}", askId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("문의 삭제에 성공했습니다."));
    }
}