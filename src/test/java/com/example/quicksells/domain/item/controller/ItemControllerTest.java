package com.example.quicksells.domain.item.controller;

import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.item.model.dto.ItemInfoDto;
import com.example.quicksells.domain.item.model.request.ItemCreatedRequest;
import com.example.quicksells.domain.item.model.request.ItemUpdateRequest;
import com.example.quicksells.domain.item.model.response.ItemCreatedResponse;
import com.example.quicksells.domain.item.model.response.ItemGetDetailResponse;
import com.example.quicksells.domain.item.model.response.ItemGetListResponse;
import com.example.quicksells.domain.item.model.response.ItemUpdateResponse;
import com.example.quicksells.domain.item.service.ItemService;
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

import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    private MockMvc mockMvc;

    @Mock
    ItemService itemService;

    @InjectMocks
    ItemController itemController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "test@test.com", USER, "홍길동");

        mockMvc = MockMvcBuilders.standaloneSetup(itemController)
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
    @DisplayName("상품 등록 성공")
    void createItem_success() throws Exception {

        // given
        ItemCreatedRequest request = new ItemCreatedRequest("상품명", 100L, "내용입니다");
        String json = objectMapper.writeValueAsString(request);

        MockMultipartFile requestPart = new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile imagePart = new MockMultipartFile("files", "jpg", MediaType.IMAGE_JPEG_VALUE, "image-data".getBytes());

        ItemInfoDto sellerInfo = new ItemInfoDto("제목입니다", 100L, "내용입니다", "imageUrl", USER, LocalDateTime.now());

        ItemCreatedResponse response = new ItemCreatedResponse(1L, 1L, sellerInfo);

        when(itemService.createItem(any(AuthUser.class), any(ItemCreatedRequest.class), any())).thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/items")
                        .file(requestPart)
                        .file(imagePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("상품 등록 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("상품 상제 조회 성공 (관리자)")
    void getDetailItem_success() throws Exception {

        // given
        Long itemId = 1L;

        ItemInfoDto sellerInfo = new ItemInfoDto("제목입니다", 100L, "내용입니다", "imageUrl", USER, LocalDateTime.now());

        ItemGetDetailResponse response = new ItemGetDetailResponse(1L, 1L, sellerInfo);

        when(itemService.getDetailItem(any())).thenReturn(response);

        // when  & then
        mockMvc.perform(get("/api/admin/items/{id}", itemId)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 조회 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("상품 전체 조회 성공 (관리자)")
    void getItemList_success() throws Exception {

        // given
        ItemInfoDto sellerInfo1 = new ItemInfoDto("상품1", 100L, "내용입니다1", "imageUrl1", USER, LocalDateTime.now());
        ItemInfoDto sellerInfo2 = new ItemInfoDto("상품2", 100L, "내용입니다2", "imageUrl2", USER, LocalDateTime.now());

        ItemGetListResponse item1 = new ItemGetListResponse(1L, 1L, sellerInfo1);
        ItemGetListResponse item2 = new ItemGetListResponse(2L, 1L, sellerInfo2);
        List<ItemGetListResponse> itemList = List.of(item1, item2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemGetListResponse> pageResponse = new PageImpl<>(itemList, pageable,itemList.size());

        when(itemService.getAll(any(Pageable.class))).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/items")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 목록 조회 성공하셨습니다."))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("나의 등록된 상품 상세 조회 성공")
    void getMyDetailItem_success() throws Exception {

        // given
        Long itemId = 1L;

        ItemInfoDto sellerInfo = new ItemInfoDto("제목입니다", 100L, "내용입니다", "imageUrl", USER, LocalDateTime.now());

        ItemGetDetailResponse response = new ItemGetDetailResponse(1L, 1L, sellerInfo);

        when(itemService.getMyDetail(any(), any(AuthUser.class))).thenReturn(response);

        // when  & then
        mockMvc.perform(get("/api/my/items/{id}", itemId)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("나의 등록 상품 조회 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("나의 등록된 상품 전체 조회 성공")
    void getMyItemList_success() throws Exception {

        // given
        ItemInfoDto sellerInfo1 = new ItemInfoDto("상품1", 100L, "내용입니다1", "imageUrl1", USER, LocalDateTime.now());
        ItemInfoDto sellerInfo2 = new ItemInfoDto("상품2", 100L, "내용입니다2", "imageUrl2", USER, LocalDateTime.now());

        ItemGetListResponse item1 = new ItemGetListResponse(1L, 1L, sellerInfo1);
        ItemGetListResponse item2 = new ItemGetListResponse(2L, 1L, sellerInfo2);
        List<ItemGetListResponse> itemList = List.of(item1, item2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemGetListResponse> pageResponse = new PageImpl<>(itemList, pageable,itemList.size());

        when(itemService.getMyItemList(any(AuthUser.class) ,any(Pageable.class))).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/my/items")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("나의 등록 상품 조회 성공하셨습니다."))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("나의 등록된 상품 수정 성공")
    void updateMyItem_success() throws Exception {

        // given
        Long itemId = 1L;

        ItemUpdateRequest request = new ItemUpdateRequest("상품명", 100L, "내용입니다", false);
        String json = objectMapper.writeValueAsString(request);

        MockMultipartFile requestPart = new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile imagePart = new MockMultipartFile("files", "jpg", MediaType.IMAGE_JPEG_VALUE, "image-data".getBytes());

        ItemInfoDto sellerInfo = new ItemInfoDto("상품명", 100L, "내용입니다", "imageUrl", USER, LocalDateTime.now());

        ItemUpdateResponse response = new ItemUpdateResponse(1L, 1L, sellerInfo);

        when(itemService.updateItem(any(AuthUser.class), any(), any(ItemUpdateRequest.class), any())).thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/items/{id}", itemId)
                        .file(requestPart)
                        .file(imagePart)
                        .with(requestProcessor -> {
                            requestProcessor.setMethod("PATCH");
                            return requestProcessor;
                        })
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 수정 성공하셨습니다."))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("나의 등록된 상품 삭제 성공")
    void deleteMyItem_success() throws Exception {

        // given
        Long itemId = 1L;

        // when & then
        mockMvc.perform(delete("/api/items/{id}", itemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 삭제 성공하셨습니다."));

    }

}