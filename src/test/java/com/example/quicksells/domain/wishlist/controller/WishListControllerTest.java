package com.example.quicksells.domain.wishlist.controller;

import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.wishlist.entity.WishList;
import com.example.quicksells.domain.wishlist.model.request.OneWishListDeleteRequest;
import com.example.quicksells.domain.wishlist.model.request.WishListCreateRequest;
import com.example.quicksells.domain.wishlist.model.response.MyWishListGetAllResponse;
import com.example.quicksells.domain.wishlist.model.response.WishListCreateResponse;
import com.example.quicksells.domain.wishlist.service.WishListService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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

import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class WishListControllerTest {

    @InjectMocks
    WishListController wishListController;

    @Mock
    WishListService wishListService;

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

    private WishList newWishList;

    private List<WishList> newWishLists = new ArrayList<>();

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

        newWishList = new WishList(buyer, newAuction);
        ReflectionTestUtils.setField(newWishList, "id", 1L);

        newWishLists.add(newWishList);

        authUser = new AuthUser(buyer.getId(), buyer.getEmail(), buyer.getRole(), buyer.getName());

        mockMvc = MockMvcBuilders.standaloneSetup(wishListController)
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
    @DisplayName("관심 목록 등록 성공 테스트")
    void save_wish_list() throws Exception {

        // given
        WishListCreateRequest request = new WishListCreateRequest(3L, 1L);

        String json = objectMapper.writeValueAsString(request);

        Long buyerId = request.getBuyerId();

        Long auctionId = request.getAuctionId();

        WishListCreateResponse response = new WishListCreateResponse(1L, buyerId, auctionId, LocalDateTime.now(Clock.systemDefaultZone()));

        when(wishListService.saveWishList(any())).thenReturn(response);

        // when&then
        mockMvc.perform(post("/api/wishList")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(response.getId()))
                .andExpect(jsonPath("$.data.buyerId").value(buyerId))
                .andExpect(jsonPath("$.data.auctionId").value(auctionId))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.message").value("관심 목록 등록에 성공했습니다."));
    }

    @Test
    @DisplayName("내 관심 목록 조회 성공 테스트")
    void get_all_wish_list() throws Exception {

        // given
        Long buyerId = buyer.getId();

        Pageable pageable = PageRequest.of(0, 10);

        Slice<WishList> wishListSlice = new SliceImpl<>(newWishLists, pageable, false);

        Slice<MyWishListGetAllResponse> responses = wishListSlice.map(MyWishListGetAllResponse::from);

        when(wishListService.getAllMyWishList(any(), any(), any())).thenReturn(responses);

        // when&then
        mockMvc.perform(get("/api/wishList")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("buyerId", buyerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(responses.getContent().get(0).getId()))
                .andExpect(jsonPath("$.data.content[0].buyerId").value(buyerId))
                .andExpect(jsonPath("$.message").value("내 관심 목록 조회에 성공했습니다."));
    }

    @Test
    @DisplayName("내 관심 목록 삭제 성공 테스트")
    void delete_wish_list() throws Exception {

        // given
        OneWishListDeleteRequest request = new OneWishListDeleteRequest(buyer.getId(), 1);

        wishListService.deleteMyWishList(any(), any(), any());

        // when&then
        mockMvc.perform(delete("/api/wishList")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("buyerId", request.getBuyerId().toString())
                .param("index", String.valueOf(request.getIndex())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value(request.getIndex() + "번째 관심 목록 삭제에 성공했습니다"));
    }
}