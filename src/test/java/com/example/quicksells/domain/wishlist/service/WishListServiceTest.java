package com.example.quicksells.domain.wishlist.service;

import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auction.repository.AuctionRepository;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import com.example.quicksells.domain.wishlist.entity.WishList;
import com.example.quicksells.domain.wishlist.model.request.OneWishListDeleteRequest;
import com.example.quicksells.domain.wishlist.model.request.WishListCreateRequest;
import com.example.quicksells.domain.wishlist.model.response.MyWishListGetAllResponse;
import com.example.quicksells.domain.wishlist.model.response.WishListCreateResponse;
import com.example.quicksells.domain.wishlist.repository.WishListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.quicksells.common.enums.ExceptionCode.*;
import static com.example.quicksells.common.enums.UserRole.ADMIN;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.example.quicksells.common.enums.UserRole.USER;

@ExtendWith(MockitoExtension.class)
class WishListServiceTest {

    @InjectMocks
    WishListService wishListService;

    @Mock
    WishListRepository wishListRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AuctionRepository auctionRepository;

    private User seller;

    private User buyerA;

    private User buyerB;

    private User admin;

    private AuthUser authUser;

    private Item newItem;

    private Appraise newAppraise;

    private Auction newAuction;

    private WishList newWishList;


    /**
     * 초기 데이터
     */
    @BeforeEach
    void setUp() {

        // 상품 판매자 아이디 1
        seller = new User("test1@test.com", "encodedPassword1", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        ReflectionTestUtils.setField(seller, "id", 1L);
        ReflectionTestUtils.setField(seller, "role", USER);

        // 인증 성공용 구매자 아이디 2
        buyerA = new User("test2@test.com", "encodedPassword2", "심청", "010-0000-2222", "서울시 강남구", "20020101");
        ReflectionTestUtils.setField(buyerA, "id", 2L);
        ReflectionTestUtils.setField(buyerA, "role", USER);

        // 인증 실패용 구매자 아이디 3
        buyerB = new User("test3@test.com", "encodedPassword3", "흥부", "010-0000-3333", "서울시 강북구", "20030101");
        ReflectionTestUtils.setField(buyerB, "id", 3L);
        ReflectionTestUtils.setField(buyerB, "role", USER);

        // 관리자 아이디 4
        admin = new User("test4@test.com", "encodedPassword4", "놀부", "010-0000-4444", "서울시 강동구", "20040101");
        ReflectionTestUtils.setField(admin, "id", 4L);
        ReflectionTestUtils.setField(admin, "role", ADMIN);

        // 인증 유저 아이디 2
        authUser = new AuthUser(buyerA.getId(), buyerA.getEmail(), buyerA.getRole(), buyerA.getName());

        // 판매자 아이디 1의 아이템 아이디 1
        newItem = new Item(seller, "아이폰 15 pro", 5000L, "사용감이 적음", "IMG.img");
        ReflectionTestUtils.setField(newItem, "id", 1L);

        newAppraise = new Appraise(admin, newItem, 10000, false);
        ReflectionTestUtils.setField(newAppraise, "id", 1L);

        newAuction = new Auction(newAppraise, newAppraise.getBidPrice());
        ReflectionTestUtils.setField(newAuction, "id", 1L);

        newWishList = new WishList(buyerA, newAuction);
        ReflectionTestUtils.setField(newWishList, "id", 1L);
    }


    /// 성공 테스트 ///

    @Test
    @DisplayName("관심 목록 등록 성공 테스트")
    void save_wish_list() {

        // given
        WishListCreateRequest request = new WishListCreateRequest(2L, 1L);

        when(userRepository.findById(any())).thenReturn(Optional.of(buyerA));

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.of(newAuction));

        WishList saveWishList = new WishList(buyerA, newAuction);

        when(wishListRepository.save(any())).thenReturn(saveWishList);
        ReflectionTestUtils.setField(saveWishList, "id", 1L);

        // when
        WishListCreateResponse result = wishListService.saveWishList(request);

        // then
        assertThat(result.getBuyerId()).isEqualTo(request.getBuyerId());
        assertThat(result.getAuctionId()).isEqualTo(request.getAuctionId());

        verify(userRepository, times(1)).findById(any());
        verify(auctionRepository, times(1)).findByIdAndStatusAndEndTimeAfter(any(), any(), any());
        verify(wishListRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("내 관심 목록 조회 성공 테스트")
    void get_my_wish_list() {

        // given
        Long requestBuyerId = 2L;

        Pageable pageable = PageRequest.of(0, 10);

        List<WishList> myWishLists = new ArrayList<>();

        myWishLists.add(newWishList);

        boolean hasNext = myWishLists.size() < pageable.getPageSize() + 1;

        SliceImpl<WishList> myWishListSlice = new SliceImpl<>(myWishLists, pageable, hasNext);

        when(wishListRepository.myWishListSearch(any(), any())).thenReturn(myWishListSlice);

        // when
        Slice<MyWishListGetAllResponse> result = wishListService.getAllMyWishList(authUser, requestBuyerId, pageable);

        // then
        assertThat(result.getContent().get(0).getBuyerId()).isEqualTo(requestBuyerId);

        verify(wishListRepository, times(1)).myWishListSearch(any(), any());
    }

    @Test
    @DisplayName("인덱스 번호에 해당되는 내 관심 목록 삭제 성공 테스트")
    void index_number_delete_my_wish_list() {

        //given
        OneWishListDeleteRequest request = new OneWishListDeleteRequest(2L, 1);

        Pageable pageable = PageRequest.of(0, 10);

        List<WishList> myWishList = new ArrayList<>();

        myWishList.add(newWishList);

        boolean hasNext = myWishList.size() < pageable.getPageSize() + 1;

        SliceImpl<WishList> myWishListSlice = new SliceImpl<>(myWishList, pageable, hasNext);

        when(wishListRepository.myWishListSearch(any(), any())).thenReturn(myWishListSlice);

        // when
        wishListService.deleteMyWishList(authUser, request, pageable);

        // then
        verify(wishListRepository, times(1)).myWishListSearch(any(), any());
        verify(wishListRepository, times(1)).delete(any());
    }


    /// 송공 테스트 ///


    /// 실패 테스트 ///

    @Test
    @DisplayName("관심 목록으로 등록할 경매의 판매자 검증 실패 테스트")
    void validate_seller() {

        // given
        WishListCreateRequest request = new WishListCreateRequest(1L, 1L);

        when(userRepository.findById(any())).thenReturn(Optional.of(seller));

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.of(newAuction));

        // when&then
        assertThatThrownBy(() -> wishListService.saveWishList(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(SELF_WISH_NOT_ALLOWED.getMessage());

        verify(userRepository, times(1)).findById(any());
        verify(auctionRepository, times(1)).findByIdAndStatusAndEndTimeAfter(any(), any(), any());
    }

    @Test
    @DisplayName("중복 검증 실패 테스트")
    void deduplication_wishList() {

        // given
        WishListCreateRequest request = new WishListCreateRequest(2L, 1L);

        when(userRepository.findById(any())).thenReturn(Optional.of(buyerA));

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.of(newAuction));

        when(wishListRepository.existsByBuyerAndAuction(any(), any())).thenReturn(true);

        // when&then
        assertThatThrownBy(() -> wishListService.saveWishList(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(CONFLICT_WISHLIST.getMessage());

        verify(userRepository, times(1)).findById(any());
        verify(auctionRepository, times(1)).findByIdAndStatusAndEndTimeAfter(any(), any(), any());
        verify(wishListRepository, times(1)).existsByBuyerAndAuction(any(), any());
    }

    @Test
    @DisplayName("다른 유저의 관심 목록 접근 권한 실패 테스트")
    void validate_user_authority() {

        // given
        Long requestBuyerId = 3L;

        Pageable pageable = PageRequest.of(0, 10);

        //  when&then
        assertThatThrownBy(() -> wishListService.getAllMyWishList(authUser, requestBuyerId, pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ACCESS_DENIED_EXCEPTION_WISHLIST.getMessage());
    }

    @Test
    @DisplayName("내 관심 목록의 인덱스 번호 검증 실패 테스트")
    void validate_index_number_delete_my_wish_list() {

        //given
        OneWishListDeleteRequest request = new OneWishListDeleteRequest(2L, 2);

        Pageable pageable = PageRequest.of(0, 10);

        List<WishList> myWishList = new ArrayList<>();

        myWishList.add(newWishList);

        boolean hasNext = myWishList.size() < pageable.getPageSize() + 1;

        SliceImpl<WishList> myWishListSlice = new SliceImpl<>(myWishList, pageable, hasNext);

        when(wishListRepository.myWishListSearch(any(), any())).thenReturn(myWishListSlice);

        // when&then
        assertThatThrownBy(() -> wishListService.deleteMyWishList(authUser, request, pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_EXIST_ONE_WISHLIST.getMessage());

        verify(wishListRepository, times(1)).myWishListSearch(any(), any());
        verify(wishListRepository, never()).delete(any());

    }

    ///  실패 테스트 ///
}