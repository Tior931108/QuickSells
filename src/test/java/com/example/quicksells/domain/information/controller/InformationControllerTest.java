package com.example.quicksells.domain.information.controller;

import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.information.model.request.InformationCreateRequest;
import com.example.quicksells.domain.information.model.request.InformationUpdateRequest;
import com.example.quicksells.domain.information.model.response.InformationCreateResponse;
import com.example.quicksells.domain.information.model.response.InformationGetAllResponse;
import com.example.quicksells.domain.information.model.response.InformationGetResponse;
import com.example.quicksells.domain.information.model.response.InformationUpdateResponse;
import com.example.quicksells.domain.information.service.InformationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.quicksells.common.enums.UserRole.ADMIN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InformationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InformationService informationService;

    @InjectMocks
    private InformationController informationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "test@test.com", ADMIN, "홍길동");

        mockMvc = MockMvcBuilders.standaloneSetup(informationController)
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
    @DisplayName("공지사항 생성 성공")
    void createInformation_success() throws Exception {

        // given
        InformationCreateRequest request = new InformationCreateRequest("제목입니다", "내용입니다");
        String json = objectMapper.writeValueAsString(request);

        MockMultipartFile requestPart = new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile imagePart = new MockMultipartFile("image", "jpg", MediaType.IMAGE_JPEG_VALUE, "image-data".getBytes());

        InformationCreateResponse response = new InformationCreateResponse(1L, 1L, "제목입니다", "내용입니다", "imageUrl", LocalDateTime.now());

        when(informationService.create(any(AuthUser.class), any(InformationCreateRequest.class), any())).thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/admin/informations")
                        .file(requestPart)
                        .file(imagePart)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("공지사항 생성 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("제목입니다"))
                .andExpect(jsonPath("$.data.imageUrl").value("imageUrl"));
    }

    @Test
    @DisplayName("공지사항 단건 조회 성공")
    void getOneInformation_success() throws Exception {

        // given
        Long informationId = 1L;

        InformationGetResponse response = new InformationGetResponse(informationId, 1L, "제목입니다", "내용입니다", "imageUrl", LocalDateTime.now(), LocalDateTime.now());

        when(informationService.getOne(any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/informations/{id}", informationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공지사항 단건 조회 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.adminId").value(1L))
                .andExpect(jsonPath("$.data.title").value("제목입니다"))
                .andExpect(jsonPath("$.data.description").value("내용입니다"))
                .andExpect(jsonPath("$.data.imageUrl").value("imageUrl"));
    }

    @Test
    @DisplayName("공지사항 전제 조회 성공")
    void getAllInformation_success() throws Exception {

        // given
        InformationGetAllResponse information1 = new InformationGetAllResponse(1L, 1L, "제목입니다1", "내용입니다1", "imageUrl1", LocalDateTime.now(), LocalDateTime.now());
        InformationGetAllResponse information2 = new InformationGetAllResponse(2L, 2L, "제목입니다2", "내용입니다2", "imageUrl2", LocalDateTime.now(), LocalDateTime.now());
        List<InformationGetAllResponse> informationList = List.of(information1, information2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<InformationGetAllResponse> pageResponse = new PageImpl<>(informationList, pageable,informationList.size());

        when(informationService.getAll(any(Pageable.class))).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/informations")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공지사항 전체 조회 성공하셨습니다."))
                .andExpect(jsonPath("$.data.content[0].title").value("제목입니다1"))
                .andExpect(jsonPath("$.data.content[1].title").value("제목입니다2"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("공지사항 수정 성공")
    void updateInformation_success() throws Exception {

        // given
        Long informationId = 1L;
        InformationUpdateRequest request = new InformationUpdateRequest("수정 제목입니다", "수정 내용입니다", false);
        String json = objectMapper.writeValueAsString(request);

        MockMultipartFile requestPart = new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile imagePart = new MockMultipartFile("image", "update.jpg", MediaType.IMAGE_JPEG_VALUE, "updateImage-data".getBytes());

        InformationUpdateResponse response = new InformationUpdateResponse(informationId, 1L, "수정 제목입니다", "수정 내용입니다", "imageUrl", LocalDateTime.now());

        when(informationService.update(any(Long.class), any(InformationUpdateRequest.class), any())).thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/admin/informations/{id}", informationId)
                        .file(requestPart)
                        .file(imagePart)
                        .with(requestProcessor -> {
                            requestProcessor.setMethod("PATCH");
                            return requestProcessor;
                        })
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공지사항 수정 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(informationId))
                .andExpect(jsonPath("$.data.title").value("수정 제목입니다"));
    }

    @Test
    @DisplayName("공지사항 삭제 성공")
    void deleteInformation_success() throws Exception {

        // given
        Long informationId = 1L;

        // when & then
        mockMvc.perform(delete("/api/admin/informations/{id}", informationId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공지사항 삭제 성공하셨습니다."));
    }

}