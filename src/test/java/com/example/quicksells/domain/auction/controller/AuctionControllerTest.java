package com.example.quicksells.domain.auction.controller;

import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auction.entity.AuctionHistory;
import com.example.quicksells.domain.auction.model.request.AuctionCreateRequest;
import com.example.quicksells.domain.auction.model.request.AuctionSearchFilterRequest;
import com.example.quicksells.domain.auction.model.request.AuctionUpdateRequest;
import com.example.quicksells.domain.auction.model.response.*;
import com.example.quicksells.domain.auction.service.AuctionService;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.*;
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
import java.util.ArrayList;
import java.util.List;

import static com.example.quicksells.common.enums.AuctionStatusType.AUCTIONING;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class AuctionControllerTest {

    @InjectMocks
    AuctionController auctionController;

    @Mock
    AuctionService auctionService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthUser authUser;

    private LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());

    private User seller;

    private User admin;

    private User buyer;

    private Item newItem;

    private Appraise newAppraise;

    private Auction newAuction;

    private AuctionHistory newAuctionHistory;

    private List<Auction> auctionList = new ArrayList<>();

    private List<AuctionHistory> auctionHistoryList = new ArrayList<>();


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
        ReflectionTestUtils.setField(newAuction, "updatedAt", now);

        newAuctionHistory = new AuctionHistory(newAuction, newAuction.getBuyer(), newAuction.getBidPrice(), newAuction.getUpdatedAt());
        ReflectionTestUtils.setField(newAuctionHistory, "id", 1L);

        auctionList.add(newAuction);

        auctionHistoryList.add(newAuctionHistory);

        authUser = new AuthUser(buyer.getId(), buyer.getEmail(), buyer.getRole(), buyer.getName());

        mockMvc = MockMvcBuilders.standaloneSetup(auctionController)
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
    @DisplayName("경매 등록 응답 성공 테스트")
    void create_auction() throws Exception {

        // given
        AuctionCreateRequest request = new AuctionCreateRequest(1L, 1);

        String json = objectMapper.writeValueAsString(request);

        Long appraiseId = request.getAppraiseId();

        int timeOption = request.getTimeOption();

        AuctionCreateResponse response = new AuctionCreateResponse(1L, appraiseId, 1L, "갤럭시 S25", 5000, AUCTIONING, now, now.plusDays(timeOption));

        when(auctionService.saveAuction(any())).thenReturn(response);

        // when&then
        mockMvc.perform(post("/api/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(response.getId()))
                .andExpect(jsonPath("$.data.appraiseId").value(request.getAppraiseId()))
                .andExpect(jsonPath("$.data.endTime").isNotEmpty())
                .andExpect(jsonPath("$.message").value("경매장 등록에 성공했습니다."));
    }

    @Test
    @DisplayName("경매 목록 조회 응답 성공 테스트")
    void get_all_auction() throws Exception {

        // given
        AuctionSearchFilterRequest request = new AuctionSearchFilterRequest(1000, 10000, "아이폰 15 pro");

        Pageable pageable = PageRequest.of(0, 10);

        Page<Auction> auctionPage = new PageImpl<>(auctionList, pageable, auctionList.size());

        Page<AuctionGetAllResponse> response = auctionPage.map(AuctionGetAllResponse::from);

        when(auctionService.getAllAuction(any(), any())).thenReturn(response);

        // when&then
        mockMvc.perform(get("/api/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("minBidPrice", request.getMinBidPrice().toString())
                        .param("maxBidPrice", request.getMaxBidPrice().toString())
                        .param("appraiseItemName", request.getAppraiseItemName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].itemName").value(request.getAppraiseItemName()))
                .andExpect(jsonPath("$.data.content[0].bidPrice").value(response.getContent().get(0).getBidPrice()))
                .andExpect(jsonPath("$.message").value("경매 목록 조회에 성공했습니다."));
    }

    @Test
    @DisplayName("경매 상세 조회 응답 성공 테스트")
    void get_auction() throws Exception {

        // given
        Long auctionId = newAuction.getId();

        AuctionGetResponse response = AuctionGetResponse.from(newAuction);

        when(auctionService.getAuction(any())).thenReturn(response);

        // when&then
        mockMvc.perform(get("/api/auctions/{id}", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(auctionId))
                .andExpect(jsonPath("$.message").value("경매 상세 조회에 성공했습니다."));
    }

    @Test
    @DisplayName("내 경매 입찰 내역 조회 응답 성공 테스트")
    void get_auction_history() throws Exception {

        // given
        Long buyerId = newAuction.getBuyer().getId();

        Pageable pageable = PageRequest.of(0, 10);

        Slice<AuctionHistory> auctionSlice = new SliceImpl<>(auctionHistoryList, pageable, false);

        Slice<AuctionHistoryGetAllResponse> responses = auctionSlice.map(AuctionHistoryGetAllResponse::from);

        when(auctionService.getAllAuctionHistory(any(), any(), any())).thenReturn(responses);

        // when&then
        mockMvc.perform(get("/api/auctionHistory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("buyerId", buyerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(responses.getContent().get(0).getId()))
                .andExpect(jsonPath("$.data.content[0].buyerId").value(buyerId))
                .andExpect(jsonPath("$.message").value("내 경매 입찰 내역 조회에 성공했습니다"));
    }

    @Test
    @DisplayName("경매 입찰 응답 성공 테스트")
    void update_bid_price() throws Exception {

        // given
        Long id = newAuction.getId();

        AuctionUpdateRequest request = new AuctionUpdateRequest(newAuction.getBuyer().getId(), 10000);

        String json = objectMapper.writeValueAsString(request);

        ReflectionTestUtils.setField(newAuction, "bidPrice", request.getBidPrice());

        AuctionUpdateResponse response = AuctionUpdateResponse.from(newAuction);

        when(auctionService.updateBidPrice(any(), any(), any())).thenReturn(response);

        // when&then
        mockMvc.perform(post("/api/auctions/{id}/bid", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.buyerId").value(request.getBuyerId()))
                .andExpect(jsonPath("$.data.bidPrice").value(request.getBidPrice()))
                .andExpect(jsonPath("$.message").value("상품 입찰에 성공했습니다."));
    }

    @Test
    @DisplayName("관리자 경매 삭제 테스트")
    void admin_delete_auction() throws Exception {

        // given
        Long id = newAuction.getId();

        ReflectionTestUtils.setField(newAuction, "isDeleted", true);

        auctionService.deleteAuction(any());

        // when&then
        mockMvc.perform(delete("/api/admin/auctions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("경매 삭제에 성공했습니다."));
    }
}



