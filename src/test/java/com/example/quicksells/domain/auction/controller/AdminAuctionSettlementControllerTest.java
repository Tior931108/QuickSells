package com.example.quicksells.domain.auction.controller;

import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auction.model.response.AdminAuctionSettlementRetryResponse;
import com.example.quicksells.domain.auction.service.AdminAuctionSettlementService;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.MethodParameter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.example.quicksells.common.enums.StatusType.ON_SALE;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAuctionSettlementControllerTest {

    @InjectMocks
    AdminAuctionSettlementController adminAuctionSettlementController;

    @Mock
    AdminAuctionSettlementService adminAuctionSettlementService;

    private MockMvc mockMvc;

    private LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());

    private User seller;

    private User admin;

    private User buyer;

    private Item newItem;

    private Appraise newAppraise;

    private Auction newAuction;

    private Deal newDeal;

    private AuthUser authUser;

    @BeforeEach
    void setUp() {

        seller = new User("test1@test.com", "encodedPassword1", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        ReflectionTestUtils.setField(seller, "id", 1L);

        admin = new User("test2@test.com", "encodedPassword2", "심청", "010-0000-2222", "서울시 강남구", "20020101");
        ReflectionTestUtils.setField(admin, "id", 2L);

        buyer = new User("test3@test.com", "encodedPassword3", "흥부", "010-0000-3333", "서울시 강북구", "20030101");
        ReflectionTestUtils.setField(buyer, "id", 3L);
        ReflectionTestUtils.setField(buyer, "role", USER);

        newItem = new Item(seller, "아이폰 15 pro", 2000L, "흠짐 조금 있음", "IMG");
        ReflectionTestUtils.setField(newItem, "id", 1L);

        newAppraise = new Appraise(admin, newItem, 5000, false);
        ReflectionTestUtils.setField(newAppraise, "id", 1L);

        newAuction = new Auction(newAppraise, newAppraise.getBidPrice());
        ReflectionTestUtils.setField(newAuction, "id", 1L);
        ReflectionTestUtils.setField(newAuction, "buyer", buyer);
        ReflectionTestUtils.setField(newAuction, "isDeleted", false);
        ReflectionTestUtils.setField(newAuction, "status", AuctionStatusType.SUCCESSFUL_BID);

        newDeal = new Deal(newAppraise, newAuction, ON_SALE, newAuction.getBidPrice());
        ReflectionTestUtils.setField(newDeal, "id", 1L);

        mockMvc = MockMvcBuilders.standaloneSetup(adminAuctionSettlementController)
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
    @DisplayName("관리자 낙찰 정산 재시도 성공 테스트")
    void admin_retry_settlement() throws Exception {

        // given
        Long auctionId = newAuction.getId();

        Long appraiseId = newAppraise.getId();

        AdminAuctionSettlementRetryResponse response = AdminAuctionSettlementRetryResponse.from(auctionId, appraiseId);

        when(adminAuctionSettlementService.retrySettlement(any())).thenReturn(response);

        // when&then
        mockMvc.perform(post("/api/admin/auctions/{auctionId}/settlements/retry", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settled").value(true))
                .andExpect(jsonPath("$.message").value("낙찰 정산 재시도를 성공했습니다."));
    }
}
