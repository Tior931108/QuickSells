package com.example.quicksells.domain.appraise.controller;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.domain.appraise.model.request.AppraiseAdminUpdateRequest;
import com.example.quicksells.domain.appraise.model.request.AppraiseCreateRequest;
import com.example.quicksells.domain.appraise.model.response.AppraiseAdminGetAllResponse;
import com.example.quicksells.domain.appraise.model.response.AppraiseAdminGetResponse;
import com.example.quicksells.domain.appraise.model.response.AppraiseAdminUpdateResponse;
import com.example.quicksells.domain.appraise.model.response.AppraiseCreateResponse;
import com.example.quicksells.domain.appraise.service.AppraiseService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppraiseAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppraiseService appraiseService;

    @InjectMocks
    private AppraiseAdminController appraiseAdminController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthUser authAdmin;

    @BeforeEach
    void setUp() {
        authAdmin = new AuthUser(1L, "admin@test.com", UserRole.ADMIN, "홍길동");

        mockMvc = MockMvcBuilders.standaloneSetup(appraiseAdminController)
                .setCustomArgumentResolvers(
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().equals(AuthUser.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return authAdmin;
                            }
                        },
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("감정 생성 성공 - 관리자")
    void createAdminAppraise_Success() throws Exception {
        // given
        Long itemId = 1L;
        AppraiseCreateRequest request = new AppraiseCreateRequest(100000);

        AppraiseCreateResponse response = new AppraiseCreateResponse(1L, 1L, "admin", itemId, "테스트 상품", 100000, false, LocalDateTime.now());

        when(appraiseService.createAppraise(eq(itemId), any(AppraiseCreateRequest.class), any(AuthUser.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/items/{itemId}/appraises", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("감정 생성에 성공했습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andDo(print());

        verify(appraiseService, times(1)).createAppraise(eq(itemId), any(AppraiseCreateRequest.class), any(AuthUser.class));
    }

    @Test
    @DisplayName("관리자 본인 감정 목록 전체 조회 성공")
    void getMyAdminAppraises_Success() throws Exception {
        // given
        Long adminId = 1L;

        AppraiseAdminGetAllResponse response1 = new AppraiseAdminGetAllResponse(1L, "seller", 1L, "테스트 상품1", 1L, 100000, AppraiseStatus.PENDING, false, LocalDateTime.now());

        AppraiseAdminGetAllResponse response2 = new AppraiseAdminGetAllResponse(2L, "seller", 2L, "테스트 상품2", 2L, 120000, AppraiseStatus.PENDING, false, LocalDateTime.now());

        List<AppraiseAdminGetAllResponse> responseList = List.of(response1, response2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<AppraiseAdminGetAllResponse> pageResponse = new PageImpl<>(responseList, pageable, responseList.size());

        when(appraiseService.getMyAdminAppraises(eq(adminId), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/appraises")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("감정 전체 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andDo(print());

        verify(appraiseService, times(1)).getMyAdminAppraises(eq(adminId), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 본인 감정 목록 조회 성공 - 상태 필터링")
    void getMyAdminAppraises_Success_WithStatusFilter() throws Exception {
        // given
        Long adminId = 1L;
        AppraiseStatus status = AppraiseStatus.PENDING;

        AppraiseAdminGetAllResponse response = new AppraiseAdminGetAllResponse(1L, "seller", 1L, "테스트 상품1", 1L, 100000, status, false, LocalDateTime.now());

        List<AppraiseAdminGetAllResponse> responseList = List.of(response);
        Pageable pageable = PageRequest.of(0, 10);
        Page<AppraiseAdminGetAllResponse> pageResponse = new PageImpl<>(responseList, pageable, responseList.size());

        when(appraiseService.getMyAdminAppraises(eq(adminId), eq(status), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/appraises")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "PENDING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("감정 전체 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andDo(print());

        verify(appraiseService, times(1)).getMyAdminAppraises(eq(adminId), eq(status), any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 본인 감정 상세 조회 성공")
    void getMyAdminAppraiseDetail_Success() throws Exception {
        // given
        Long appraiseId = 1L;
        Long adminId = 1L;

        // SellerDto
        AppraiseAdminGetResponse.SellerDto sellerDto =
                new AppraiseAdminGetResponse.SellerDto(
                        2L,               // sellerId
                        "seller@test.com",       // email
                        "판매자"                  // sellerName
                );

        // ItemDetailDto
        AppraiseAdminGetResponse.ItemDetailDto itemDetailDto =
                new AppraiseAdminGetResponse.ItemDetailDto(
                        1L,                      // itemId
                        "테스트 상품",            // name
                        "상품 설명입니다",         // description
                        100000L,                 // hopePrice
                        "image-url.jpg"          // imageUrl
                );

        // AppraiseAdminGetResponse
        AppraiseAdminGetResponse response = new AppraiseAdminGetResponse(
                sellerDto,                       // seller
                itemDetailDto,                   // item
                appraiseId,                      // appraiseId
                100000,                          // bidPrice
                AppraiseStatus.PENDING,          // status
                false,                           // isSelected
                LocalDateTime.now()              // createdAt
        );

        when(appraiseService.getMyAdminAppraiseDetail(eq(appraiseId), eq(adminId)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/appraises/{id}", appraiseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("감정 단 건 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andDo(print());

        verify(appraiseService, times(1)).getMyAdminAppraiseDetail(eq(appraiseId), eq(adminId));
    }

    @Test
    @DisplayName("관리자 본인 감정가 수정 성공")
    void updateMyAdminAppraise_Success() throws Exception {
        // given
        Long appraiseId = 1L;
        Long adminId = 1L;
        AppraiseAdminUpdateRequest request = new AppraiseAdminUpdateRequest(150000);

        AppraiseAdminUpdateResponse response = new AppraiseAdminUpdateResponse(appraiseId, 150000);

        when(appraiseService.updateMyAdminAppraise(eq(appraiseId), any(AppraiseAdminUpdateRequest.class), eq(adminId)))
                .thenReturn(response);

        // when & then
        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/api/admin/appraises/{id}", appraiseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("감정가 수정에 성공했습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andDo(print());

        verify(appraiseService, times(1)).updateMyAdminAppraise(eq(appraiseId), any(AppraiseAdminUpdateRequest.class), eq(adminId));
    }

    @Test
    @DisplayName("감정 삭제 성공 - 관리자")
    void deleteAdminAppraise_Success() throws Exception {
        // given
        Long itemId = 1L;

        doNothing().when(appraiseService).deleteAppraise(eq(itemId), any(AuthUser.class));

        // when & then
        mockMvc.perform(delete("/api/admin/items/{itemId}/appraises", itemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("감정 삭제에 성공했습니다."))
                .andDo(print());

        verify(appraiseService, times(1)).deleteAppraise(eq(itemId), any(AuthUser.class));
    }
}