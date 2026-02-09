package com.example.quicksells.domain.auction.service;

import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auction.model.response.AdminAuctionSettlementRetryResponse;
import com.example.quicksells.domain.auction.repository.AuctionRepository;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.deal.repository.DealRepository;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.payment.service.AuctionSettlementService;
import com.example.quicksells.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.example.quicksells.common.enums.AuctionStatusType.SUCCESSFUL_BID;
import static com.example.quicksells.common.enums.AuctionStatusType.UNSUCCESSFUL_BID;
import static com.example.quicksells.common.enums.ExceptionCode.*;
import static com.example.quicksells.common.enums.StatusType.ON_SALE;
import static com.example.quicksells.common.enums.StatusType.SOLD;
import static com.example.quicksells.common.enums.UserRole.ADMIN;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuctionSettlementServiceTest {

    @InjectMocks
    AdminAuctionSettlementService adminAuctionSettlementService;

    @Mock
    AuctionRepository auctionRepository;

    @Mock
    DealRepository dealRepository;

    @Mock
    AuctionSettlementService auctionSettlementService;

    private User admin;

    private User seller;

    private User buyerA;

    private User buyerB;

    private User buyerC;

    private User buyerD;

    private Item newItemA;

    private Item newItemB;

    private Item newItemC;

    private Item newItemD;

    private Appraise newAppraiseA;

    private Appraise newAppraiseB;

    private Appraise newAppraiseC;

    private Appraise newAppraiseD;

    private Auction newAuctionA;

    private Auction newAuctionB;

    private Auction newAuctionC;

    private Auction newAuctionD;

    private Deal newDealA;

    private Deal newDealB;

    private Deal newDealC;


    /**
     * 초기 데이터
     */
    @BeforeEach
    void setUp() {

        /**
         * 관리자, 판매자
         */
        admin = new User("test1@test.com", "encodedPassword1", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "role", ADMIN);

        seller = new User("test2@test.com", "encodedPassword2", "심청", "010-0000-2222", "서울시 강남구", "20020101");
        ReflectionTestUtils.setField(seller, "id", 2L);
        ReflectionTestUtils.setField(admin, "role", USER);


        /// 낙찰 경매 정산 재시도 성공용 ///

        buyerA = new User("test3@test.com", "encodedPassword3", "흥부", "010-0000-3333", "서울시 강북구", "20030101");
        ReflectionTestUtils.setField(seller, "id", 3L);
        ReflectionTestUtils.setField(admin, "role", USER);

        newItemA = new Item(seller, "아이폰 15 pro", 5000L, "사용감이 적음", "IMG.img");
        ReflectionTestUtils.setField(newItemA, "id", 1L);

        newAppraiseA = new Appraise(admin, newItemA, 10000, true);
        ReflectionTestUtils.setField(newAppraiseA, "id", 1L);

        // 낙철 경매
        newAuctionA = new Auction(newAppraiseA, newAppraiseA.getBidPrice());
        ReflectionTestUtils.setField(newAuctionA, "id", 1L);
        ReflectionTestUtils.setField(newAuctionA, "buyer", buyerA);
        ReflectionTestUtils.setField(newAuctionA, "status", SUCCESSFUL_BID);

        // 정산 되지 않은 낙찰 경매
        newDealA = new Deal(newAppraiseA, newAuctionA, ON_SALE, newAuctionA.getBidPrice());
        ReflectionTestUtils.setField(newDealA, "id", 1L);

        /// 낙찰 경매 정산 재시도 성공용 ///


        /// 유찰 경매 정산 재시도 실패용 ///

        buyerB = new User("test4@test.com", "encodedPassword4", "놀부", "010-0000-4444", "서울시 강동구", "20040101");
        ReflectionTestUtils.setField(buyerB, "id", 4L);
        ReflectionTestUtils.setField(buyerB, "role", USER);

        newItemB = new Item(seller, "갤럭시 s26", 5000L, "사용감이 많음", "IMG.img");
        ReflectionTestUtils.setField(newItemB, "id", 2L);

        newAppraiseB = new Appraise(admin, newItemB, 10000, true);
        ReflectionTestUtils.setField(newAppraiseB, "id", 2L);

        newAuctionB = new Auction(newAppraiseB, newAppraiseB.getBidPrice());
        ReflectionTestUtils.setField(newAuctionB, "id", 2L);
        ReflectionTestUtils.setField(newAuctionB, "buyer", buyerB);
        ReflectionTestUtils.setField(newAuctionB, "status", UNSUCCESSFUL_BID);

        /// 유찰 경매 정산 재시도 실패용 ///


        /// 정산 된 낙찰 경매 정산 재시도 실패용 ///

        buyerC = new User("test5@test.com", "encodedPassword5", "신사임당", "010-0000-5555", "서울시 은평구", "20050101");
        ReflectionTestUtils.setField(buyerC, "id", 5L);
        ReflectionTestUtils.setField(buyerC, "role", USER);

        newItemC = new Item(seller, "샤오미 s20", 5000L, "중국산 보급제", "IMG.img");
        ReflectionTestUtils.setField(newItemC, "id", 3L);

        newAppraiseC = new Appraise(admin, newItemC, 10000, true);
        ReflectionTestUtils.setField(newAppraiseC, "id", 3L);

        newAuctionC = new Auction(newAppraiseC, newAppraiseC.getBidPrice());
        ReflectionTestUtils.setField(newAuctionC, "id", 3L);
        ReflectionTestUtils.setField(newAuctionC, "buyer", buyerC);
        ReflectionTestUtils.setField(newAuctionC, "status", SUCCESSFUL_BID);

        newDealB = new Deal(newAppraiseC, newAuctionC, SOLD, newAuctionC.getBidPrice());
        ReflectionTestUtils.setField(newDealB, "id", 2L);

        /// 정산 된 낙찰 경매 정산 재시도 실패용 ///


        /// 포인트 잔액이 부족한 구매자의 낙찰 경매 정산 재시도 실패용 ///

        buyerD = new User("test6@test.com", "encodedPassword6", "세종대왕", "010-0000-6666", "서울시 중구", "20060101");
        ReflectionTestUtils.setField(buyerD, "id", 1L);
        ReflectionTestUtils.setField(buyerD, "role", USER);

        newItemD = new Item(seller, "LG 옵티머스 프라임", 5000L, "단종된 브랜드", "IMG.img");
        ReflectionTestUtils.setField(newItemD, "id", 4L);

        newAppraiseD = new Appraise(admin, newItemD, 10000, true);
        ReflectionTestUtils.setField(newAppraiseD, "id", 4L);

        newAuctionD = new Auction(newAppraiseD, newAppraiseD.getBidPrice());
        ReflectionTestUtils.setField(newAuctionD, "id", 4L);
        ReflectionTestUtils.setField(newAuctionD, "buyer", buyerD);
        ReflectionTestUtils.setField(newAuctionD, "status", SUCCESSFUL_BID);

        newDealC = new Deal(newAppraiseD, newAuctionD, ON_SALE, newAuctionD.getBidPrice());
        ReflectionTestUtils.setField(newDealC, "id", 50000L);

        /// 포인트 잔액이 부족한 구매자의 낙찰 경매 정산 재시도 실패용 ///
    }

    /// 성공 테스트 ///

    @Test
    @DisplayName("관리자 낙찰 경매 정산 재시도 성공 테스트")
    void admin_successful_bid_auction_retry_settlement() {

        // given
        Long requestAuctionId = 1L;

        when(auctionRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(newAuctionA));

        when(dealRepository.findByAppraiseId(any())).thenReturn(Optional.of(newDealA));

        // when
        AdminAuctionSettlementRetryResponse result = adminAuctionSettlementService.retrySettlement(requestAuctionId);

        // then
        assertThat(result.getAuctionId()).isEqualTo(newAuctionA.getId());
        assertThat(result.getDealId()).isEqualTo(newDealA.getId());

        verify(auctionRepository, times(1)).findByIdAndIsDeletedFalse(any());
        verify(dealRepository, times(1)).findByAppraiseId(any());
        verify(auctionSettlementService, times(1)).settleSuccessfulAuction(any());
    }

    /// 성공 테스트 ///


    /// 실패 테스트 ///

    @Test
    @DisplayName("경매 상태 검증 실패 테스트")
    void validate_auction_status_auction() {

        // given
        Long requestAuctionId = 2L;

        when(auctionRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(newAuctionB));

        // when&then
        assertThatThrownBy(() -> adminAuctionSettlementService.retrySettlement(requestAuctionId))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_FOUND_AUCTION.getMessage());

        verify(auctionRepository, times(1)).findByIdAndIsDeletedFalse(any());
        verify(dealRepository, never()).findByAppraiseId(any());
    }

    @Test
    @DisplayName("낙찰된 경매의 정산 상태 검증 실패 테스트")
    void validate_success_bid_auction_settlement_status() {

        // given
        Long requestAuctionId = 3L;

        when(auctionRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(newAuctionC));

        when(dealRepository.findByAppraiseId(any())).thenReturn(Optional.of(newDealB));

        // when
        AdminAuctionSettlementRetryResponse result = adminAuctionSettlementService.retrySettlement(requestAuctionId);

        // then
        assertThat(result.getAuctionId()).isEqualTo(requestAuctionId);
        assertThat(result.getDealId()).isEqualTo(newDealB.getId());
        assertThat(result.isSettled()).isTrue();

        verify(auctionRepository, times(1)).findByIdAndIsDeletedFalse(any());
        verify(dealRepository, times(1)).findByAppraiseId(any());
    }

    @Test
    @DisplayName("포인트 잔액 검증 실패 테스트")
    void validate_insufficient_balance_buyer_settlement() {

        // given
        Long requestAuctionId = 4L;

        when(auctionRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(newAuctionD));

        when(dealRepository.findByAppraiseId(any())).thenReturn(Optional.of(newDealC));

        doThrow(new CustomException(INSUFFICIENT_BALANCE))
                .when(auctionSettlementService).settleSuccessfulAuction(any());

        // when&then
        assertThatThrownBy(() -> adminAuctionSettlementService.retrySettlement(requestAuctionId))
                .isInstanceOf(CustomException.class)
                .hasMessage(AUCTION_SETTLEMENT_PAYMENT_REQUIRED.getMessage());

        verify(auctionRepository, times(1)).findByIdAndIsDeletedFalse(any());
        verify(dealRepository, times(1)).findByAppraiseId(any());
        verify(auctionSettlementService, times(1)).settleSuccessfulAuction(any());
    }

    @Test
    @DisplayName("낙찰 경매 정산 재시도 검증 실패 테스트")
    void validate_bid_success_full_auction_retry() {

        // given
        Long requestAuctionId = 5L;

        /**
         *  혹시 모르는 예외 발생 검증
         *  ex) 낙찰된 경매인데 구매자가 null
         */
        Auction auction = new Auction(newAppraiseA, newAppraiseA.getBidPrice());
        ReflectionTestUtils.setField(auction, "id", 5L);
        ReflectionTestUtils.setField(auction, "buyer", null);
        ReflectionTestUtils.setField(auction, "status", SUCCESSFUL_BID);

        Deal deal = new Deal(newAppraiseA, auction, ON_SALE, auction.getBidPrice());
        ReflectionTestUtils.setField(auction, "id", 4L);

        when(auctionRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(auction));

        when(dealRepository.findByAppraiseId(any())).thenReturn(Optional.of(deal));

        doThrow(CustomException.class)
                .when(auctionSettlementService).settleSuccessfulAuction(any());

        // when&then
        assertThatThrownBy(() -> adminAuctionSettlementService.retrySettlement(requestAuctionId))
                .isInstanceOf(CustomException.class);

        verify(auctionRepository, times(1)).findByIdAndIsDeletedFalse(any());
        verify(dealRepository, times(1)).findByAppraiseId(any());
        verify(auctionSettlementService, times(1)).settleSuccessfulAuction(any());
    }

    /// 실패 테스트 ///
}
