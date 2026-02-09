package com.example.quicksells.domain.auction.service;

import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.appraise.repository.AppraiseRepository;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auction.entity.AuctionHistory;
import com.example.quicksells.domain.auction.model.dto.BidInfo;
import com.example.quicksells.domain.auction.model.request.AuctionCreateRequest;
import com.example.quicksells.domain.auction.model.request.AuctionSearchFilterRequest;
import com.example.quicksells.domain.auction.model.request.AuctionUpdateRequest;
import com.example.quicksells.domain.auction.model.response.*;
import com.example.quicksells.domain.auction.repository.AuctionHistoryRepository;
import com.example.quicksells.domain.auction.repository.AuctionRepository;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.deal.service.DealService;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.service.PointWalletService;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RPatternTopic;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.PatternMessageListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.quicksells.common.enums.AuctionStatusType.AUCTIONING;
import static com.example.quicksells.common.enums.AuctionStatusType.SUCCESSFUL_BID;
import static com.example.quicksells.common.enums.ExceptionCode.*;
import static com.example.quicksells.common.enums.StatusType.ON_SALE;
import static com.example.quicksells.common.enums.UserRole.ADMIN;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @InjectMocks
    AuctionService auctionService;

    @InjectMocks
    AuctionBIdEventListenerService auctionBIdEventListenerService;

    @InjectMocks
    AuctionBidSubscriberService auctionBidSubscriberService;

    @Mock
    RedissonClient redisson;

    @Mock
    AuctionRepository auctionRepository;

    @Mock
    DealService dealService;

    @Mock
    AuctionHistoryRepository auctionHistoryRepository;

    @Mock
    AppraiseRepository appraiseRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    PointWalletService pointWalletService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    RTopic rTopic;

    @Mock
    RPatternTopic rPatternTopic;

    @Mock
    SimpMessagingTemplate messagingTemplate;


    /**
     * 객체 필드
     */
    private AuthUser authBuyer;

    private AuthUser authSeller;

    private User seller;

    private User admin;

    private User buyerA;

    private User buyerB;

    private Item newItem;

    private Appraise newAppraise;

    private Auction newAuction;

    private LocalDateTime now;

    private AuctionHistory auctionHistory;

    private PointWallet pointWallet;

    private Auction closeAuction;

    private BidInfo bidInfo;

    /**
     * 초기 데이터
     */
    @BeforeEach
    void setUp() {

        now = LocalDateTime.now(Clock.systemDefaultZone());

        // 판매자 ID 1
        seller = new User("test1@test.com", "encodedPassword1", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        ReflectionTestUtils.setField(seller, "id", 1L);
        ReflectionTestUtils.setField(seller, "role", USER);

        // 관리자 ID 2
        admin = new User("test2@test.com", "encodedPassword2", "심청", "010-0000-2222", "서울시 강남구", "20020101");
        ReflectionTestUtils.setField(admin, "id", 2L);
        ReflectionTestUtils.setField(admin, "role", ADMIN);

        // 성공 유저용 ID 3
        buyerA = new User("test3@test.com", "encodedPassword3", "흥부", "010-0000-3333", "서울시 강북구", "20030101");
        ReflectionTestUtils.setField(buyerA, "id", 3L);
        ReflectionTestUtils.setField(buyerA, "role", USER);

        // 실패 유저용 ID 4
        buyerB = new User("test4@test.com", "encodedPassword4", "놀부", "010-0000-4444", "서울시 강동구", "20040101");
        ReflectionTestUtils.setField(buyerB, "id", 4L);
        ReflectionTestUtils.setField(buyerB, "role", USER);

        // 인증 구매자 ID 3
        authBuyer = new AuthUser(buyerA.getId(), buyerA.getEmail(), buyerA.getRole(), buyerA.getName());

        // 인증 판매자 ID 1
        authSeller = new AuthUser(seller.getId(), seller.getEmail(), seller.getRole(), seller.getName());

        // 아이템 ID 1, 판매자 ID 1
        newItem = new Item(seller, "아이폰 15 pro", 5000L, "사용감이 적음", "IMG.img");
        ReflectionTestUtils.setField(newItem, "id", 1L);

        // 감정 ID 1, 관리자 ID 2, 아이템 ID 1
        newAppraise = new Appraise(admin, newItem, 10000, false);
        ReflectionTestUtils.setField(newAppraise, "id", 1L);

        // 진행중인 경매 ID 1, 감정 ID 1, 입찰가 10000원, 판매자 ID 1
        newAuction = new Auction(newAppraise, newAppraise.getBidPrice());
        ReflectionTestUtils.setField(newAuction, "id", 1L);
        ReflectionTestUtils.setField(newAuction, "buyer", buyerA);
        ReflectionTestUtils.setField(newAuction, "status", AUCTIONING);
        ReflectionTestUtils.setField(newAuction, "createdAt", now);
        ReflectionTestUtils.setField(newAuction, "updatedAt", now.plusMinutes(1));
        ReflectionTestUtils.setField(newAuction, "endTime", now.plusDays(1));
        ReflectionTestUtils.setField(newAuction, "isDeleted", false);

        // 입찰 정보
        bidInfo = new BidInfo(newAuction.getId(), newAuction.getBuyer().getName(), newAuction.getBidPrice());

        // 경매 입찰 내역 ID 1
        auctionHistory = new AuctionHistory(newAuction, buyerA, newAuction.getBidPrice(), newAuction.getUpdatedAt());
        ReflectionTestUtils.setField(auctionHistory, "id", 1L);

        // 포인트 지갑 ID 1, 구매자 ID 3, 포인트 잔액 50000
        pointWallet = new PointWallet(buyerA.getId());
        ReflectionTestUtils.setField(pointWallet, "availableBalance", 50000L);

        // 마감된 경매 ID 2, 입찰가 10000원
        closeAuction = new Auction(newAppraise, newAppraise.getBidPrice());
        ReflectionTestUtils.setField(closeAuction, "id", 2L);
        ReflectionTestUtils.setField(closeAuction, "status", SUCCESSFUL_BID);
        ReflectionTestUtils.setField(closeAuction, "createdAt", now);
        ReflectionTestUtils.setField(closeAuction, "endTime", now);
    }


    /// 성공 테스트 ///

    @Test
    @DisplayName("경매 등록 응답 성공 테스트")
    void save_auction() {

        /**
         * given
         * 경매 등록용 객체 아이디 2
         */

        Item saveItem = new Item(seller, "갤럭시 s26", 10000L, "사용감 많음", "IMG.img");
        ReflectionTestUtils.setField(saveItem, "id", 2L);

        Appraise saveAppraise = new Appraise(admin, saveItem, 10000, false);
        ReflectionTestUtils.setField(saveAppraise, "id", 2L);

        /**
         * <요청>
         * appraiseId = 2
         * timeOption = 2
         */
        AuctionCreateRequest request = new AuctionCreateRequest(2L, 2);

        when(appraiseRepository.findById(any())).thenReturn(Optional.of(saveAppraise));

        Auction saveAuction = new Auction(saveAppraise, saveAppraise.getBidPrice());

        ReflectionTestUtils.setField(saveAuction, "id", 2L);
        ReflectionTestUtils.setField(saveAuction, "createdAt", LocalDateTime.now(Clock.systemDefaultZone()));
        ReflectionTestUtils.setField(saveAuction, "endTime", saveAuction.getCreatedAt().plusDays(request.getTimeOption()));

        // when
        when(auctionRepository.save(any())).thenReturn(saveAuction);

        Deal saveDeal = new Deal(saveAppraise, saveAuction, ON_SALE, saveAuction.getBidPrice());

        when(dealService.createAuctionDeal(any(), any())).thenReturn(saveDeal);

        AuctionCreateResponse result = auctionService.saveAuction(request);

        // then
        assertThat(result.getId()).isEqualTo(saveAuction.getId());
        assertThat(result.getAppraiseId()).isEqualTo(request.getAppraiseId());
        assertThat(result.getEndTime()).isEqualTo(saveAuction.getCreatedAt().plusDays(request.getTimeOption()));

        verify(auctionRepository, times(1)).save(any(Auction.class));
        verify(dealService, times(1)).createAuctionDeal(any(Appraise.class), any(Auction.class));
    }

    @Test
    @DisplayName("경매 목록 조회 응답 성공 테스트")
    void get_all_auction() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        List<Auction> auctionList = new ArrayList<>();

        auctionList.add(newAuction);

        Page<Auction> auctionPage = new PageImpl<>(auctionList, pageable, auctionList.size());

        // when
        when(auctionRepository.auctionSearch(pageable, null)).thenReturn(auctionPage);

        Page<AuctionGetAllResponse> result = auctionService.getAllAuction(pageable, null);

        // then
        assertThat(result.getContent().get(0).getId()).isEqualTo(auctionPage.getContent().get(0).getId());

        verify(auctionRepository, times(1)).auctionSearch(pageable, null);
    }

    @Test
    @DisplayName("경매 목록 필터 적용 조회 응답 성공 테스트")
    void get_all_filtering_auction() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        List<Auction> auctionList = new ArrayList<>();

        auctionList.add(newAuction);

        Page<Auction> auctionPage = new PageImpl<>(auctionList, pageable, auctionList.size());

        /**
         * <요청>
         * MinBidPrice = 9000 between MaxBidPrice = 11000
         * appraiseItemName = 아이폰 15 pro
         */
        AuctionSearchFilterRequest request = new AuctionSearchFilterRequest(9000, 11000, "아이폰 15 pro");

        when(auctionRepository.auctionSearch(any(), any())).thenReturn(auctionPage);

        // when
        Page<AuctionGetAllResponse> result = auctionService.getAllAuction(pageable, request);

        // then
        assertThat(result.getContent().get(0).getId()).isEqualTo(newAuction.getId());
        assertThat(result.getContent().get(0).getBidPrice()).isBetween(request.getMinBidPrice(), request.getMaxBidPrice());
        assertThat(result.getContent().get(0).getItemName()).isEqualTo(request.getAppraiseItemName());

        verify(auctionRepository, times(1)).auctionSearch(eq(pageable), eq(request));
    }

    @Test
    @DisplayName("경매 상세 조회 응답 성공 테스트")
    void get_auction_by_id() {

        // given
        Long requestAuctionId = 1L;

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.of(newAuction));

        // when
        AuctionGetResponse result = auctionService.getAuction(requestAuctionId);

        // then
        assertThat(result.getId()).isEqualTo(requestAuctionId);

        verify(auctionRepository, times(1))
                .findByIdAndStatusAndEndTimeAfter(eq(requestAuctionId), eq(AUCTIONING), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("내 경매 입찰 내역 조회 응답 성공 테스트")
    void get_auction_history() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        Long requestBuyerId = 3L;

        List<AuctionHistory> auctionHistoryList = new ArrayList<>();

        auctionHistoryList.add(auctionHistory);

        boolean hasNext = auctionHistoryList.size() > pageable.getPageSize();

        SliceImpl<AuctionHistory> auctionSlice = new SliceImpl<>(auctionHistoryList, pageable, hasNext);

        when(auctionHistoryRepository.findByBuyerId(any(), any())).thenReturn(auctionSlice);

        // when
        Slice<AuctionHistoryGetAllResponse> result = auctionService.GetAllAuctionHistory(pageable, authBuyer, requestBuyerId);

        // then
        assertThat(result.getContent().get(0).getId()).isEqualTo(auctionSlice.getContent().get(0).getId());
        assertThat(result.getContent().get(0).getBuyerId()).isEqualTo(requestBuyerId);

        verify(auctionHistoryRepository, times(1)).findByBuyerId(eq(pageable), eq(requestBuyerId));
    }

    @Test
    @DisplayName("경매 입찰 응답 성공 테스트")
    void update_bid_price() {

        /**
         * given
         * <요청>
         * authBuyerId = 3
         * AuctionId = 1
         * buyerId = 3, bidPrice = 15000
         */
        Long requestAuctionId = 1L;

        AuctionUpdateRequest request = new AuctionUpdateRequest(3L, 15000);

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.of(newAuction));

        when(userRepository.findById(any())).thenReturn(Optional.of(buyerA));

        AuctionHistory saveAuctionHistory = new AuctionHistory(newAuction, buyerA, request.getBidPrice(), newAuction.getUpdatedAt());

        when(auctionHistoryRepository.save(any())).thenReturn(saveAuctionHistory);

        when(pointWalletService.getOrCreate(any())).thenReturn(pointWallet);

        eventPublisher.publishEvent(any());

        // when
        AuctionUpdateResponse result = auctionService.updateBidPrice(requestAuctionId, request, authBuyer);

        // then
        assertThat(result.getId()).isEqualTo(requestAuctionId);
        assertThat(result.getBuyerId()).isEqualTo(request.getBuyerId());
        assertThat(result.getBidPrice()).isEqualTo(request.getBidPrice());

        // 포인트 지갑 잔액 확인이 호출되었는가?
        verify(pointWalletService, times(1)).getOrCreate(request.getBuyerId());

        // 입찰 내역이 저장되었는가?
        verify(auctionHistoryRepository, times(1)).save(any(AuctionHistory.class));

        // 알림 이벤트가 발행되었는가?
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    @DisplayName("경매 삭제 성공 테스트")
    void admin_delete_auction() {

        // given
        Long requestAuctionId = 1L;

        when(auctionRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(newAuction));

        // when
        auctionService.deleteAuction(requestAuctionId);

        // then
        assertThat(requestAuctionId).isEqualTo(newAuction.getId());
        assertThat(newAuction.isDeleted()).isTrue();

        verify(auctionRepository, times(1)).findByIdAndIsDeletedFalse(requestAuctionId);
    }

    /// 성공 테스트 ///


    /// 실패 테스트 ///

    @Test
    @DisplayName("경매 중복 등록 검증 실패 테스트")
    void deduplication_save_auction() {

        // given
        AuctionCreateRequest request = new AuctionCreateRequest(1L, 1);

        when(appraiseRepository.findById(request.getAppraiseId())).thenReturn(Optional.ofNullable(newAppraise));

        when(auctionRepository.existsByAppraise(newAppraise)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> auctionService.saveAuction(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(CONFLICT_AUCTION.getMessage());

        verify(auctionRepository, times(1)).existsByAppraise(newAppraise);
        verify(auctionRepository, never()).save(any());
        verify(dealService, never()).createAuctionDeal(any(), any());
    }

    @Test
    @DisplayName("경매 상세 조회 응답 실패 테스트")
    void not_found_get_auction() {

        // given
        Long requestAuctionId = closeAuction.getId();

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.empty());

        // when&then
        assertThatThrownBy(() -> auctionService.getAuction(requestAuctionId))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_FOUND_AUCTION.getMessage());

        verify(auctionRepository, times(1)).findByIdAndStatusAndEndTimeAfter(any(), any(), any());
    }

    @Test
    @DisplayName("경매 입찰 내역 조회 유저 검증 실패 테스트")
    void validate_User_get_all_auction_history() {

        /**
         * given
         * <요청>
         * authBuyerId = 3
         * requestBuyerId = 1
         */
        Long requestBuyerId = 1L;

        Pageable pageable = PageRequest.of(0, 10);

        // when&then
        assertThatThrownBy(() -> auctionService.GetAllAuctionHistory(pageable, authBuyer, requestBuyerId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ACCESS_DENIED_ONLY_OWNER.getMessage());

        verify(auctionRepository, never()).findByIdAndStatusAndEndTimeAfter(any(), any(), any());
    }

    @Test
    @DisplayName("경매 입찰 인증 유저 권한 검증 실패 테스트")
    void validate_auth_user_bid() {

        /**
         * given
         * <요청>
         * authBuyerId = 3
         * buyerBId = 4, bidPrice = 15000
         */
        AuctionUpdateRequest request = new AuctionUpdateRequest(4L, 15000);

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.of(newAuction));

        when(userRepository.findById(any())).thenReturn(Optional.ofNullable(buyerB));

        // when&then
        assertThatThrownBy(() -> auctionService.updateBidPrice(newAuction.getId(), request, authBuyer))
                .isInstanceOf(CustomException.class)
                .hasMessage(ACCESS_DENIED_ONLY_OWNER.getMessage());

        verify(auctionRepository, times(1)).findByIdAndStatusAndEndTimeAfter(any(), any(), any());
        verify(userRepository, times(1)).findById(any());
        verify(pointWalletService, never()).getOrCreate(request.getBuyerId());
        verify(auctionHistoryRepository, never()).save(any(AuctionHistory.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("경매 입찰 유저 판매자 검증 실패 테스트")
    void validate_auction_seller_bid () {
        /**
         * given
         * <요청>
         * authSellerId = 1
         * buyerBId = 1, bidPrice = 15000
         */
        AuctionUpdateRequest request = new AuctionUpdateRequest(1L, 15000);

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.of(newAuction));

        when(userRepository.findById(any())).thenReturn(Optional.ofNullable(seller));

        // when&then
        assertThatThrownBy(() -> auctionService.updateBidPrice(newAuction.getId(), request, authSeller))
                .isInstanceOf(CustomException.class)
                .hasMessage(SELLER_CANNOT_PURCHASE_OWN_AUCTION.getMessage());

        verify(auctionRepository, times(1)).findByIdAndStatusAndEndTimeAfter(any(), any(), any());
        verify(userRepository, times(1)).findById(any());
        verify(pointWalletService, never()).getOrCreate(request.getBuyerId());
        verify(auctionHistoryRepository, never()).save(any(AuctionHistory.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("경매 입찰 검증 실패 테스트")
    void validate_bid_price() {

        /**
         * given
         * <요청>
         * authBuyerId = 3
         * buyerAId = 3, bidPrice = 5000
         */
        AuctionUpdateRequest request = new AuctionUpdateRequest(3L, 5000);

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.of(newAuction));

        when(userRepository.findById(any())).thenReturn(Optional.of(buyerA));

        // when&then
        assertThatThrownBy(() -> auctionService.updateBidPrice(newAuction.getId(), request, authBuyer))
                .isInstanceOf(CustomException.class)
                .hasMessage(BID_PRICE_TOO_LOW.getMessage());

        verify(auctionRepository, times(1)).findByIdAndStatusAndEndTimeAfter(any(), any(), any());
        verify(userRepository, times(1)).findById(any());
        verify(pointWalletService, never()).getOrCreate(request.getBuyerId());
        verify(auctionHistoryRepository, never()).save(any(AuctionHistory.class));
        verify(eventPublisher, never()).publishEvent(any());
    }


    @Test
    @DisplayName("포인트 지갑 잔액 검증 실패 테스트")
    void validate_point_wallet_balance() {
        /**
         * given
         * <요청>
         * authBuyerId = 3
         * buyerAId = 3, bidPrice = 60000
         */
        AuctionUpdateRequest request = new AuctionUpdateRequest(3L, 60000);

        when(auctionRepository.findByIdAndStatusAndEndTimeAfter(any(), any(), any())).thenReturn(Optional.of(newAuction));

        when(userRepository.findById(any())).thenReturn(Optional.of(buyerA));

        when(pointWalletService.getOrCreate(any())).thenReturn(pointWallet);

        // when&then
        assertThatThrownBy(() -> auctionService.updateBidPrice(newAuction.getId(), request, authBuyer))
                .isInstanceOf(CustomException.class)
                .hasMessage(INSUFFICIENT_BALANCE.getMessage());

        verify(auctionRepository, times(1)).findByIdAndStatusAndEndTimeAfter(any(), any(), any());
        verify(userRepository, times(1)).findById(any());
        verify(pointWalletService, times(1)).getOrCreate(request.getBuyerId());
        verify(auctionHistoryRepository, never()).save(any(AuctionHistory.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    /// 실패 테스트 ///


    /// PUB SUB 테스트///

    @Test
    @DisplayName("경매 입찰 정보 실시간 이벤트 발행 성공 테스트")
    void auction_bid_info_live_event_publisher () {

        // given
        when(redisson.getTopic(any(), any())).thenReturn(rTopic);

        when(rTopic.publish(any())).thenReturn(0L);

        // when
        auctionBIdEventListenerService.handleOrderCreatedEvent(bidInfo);

        // then
        verify(redisson, times(1)).getTopic(any(), any());
        verify(rTopic, times(1)).publish(any());
    }

    @Test
    @DisplayName("등록된 리스너에게 실시간 입찰 메시지를 웹소켓으로 전송 성공 테스트")
    void listener_live_bid_info_message_send_to_websocket() {

        // given
        when(redisson.getPatternTopic(any(), any())).thenReturn(rPatternTopic);

        ArgumentCaptor<PatternMessageListener<BidInfo>> captor = ArgumentCaptor.forClass(PatternMessageListener.class); // 리스너 캡쳐

        // when
        auctionBidSubscriberService.setUp();

        // then
        verify(rPatternTopic).addListener(eq(BidInfo.class), captor.capture());

        /**
         * given&then
         * 웹소켓 테스트
         */
        var listener = captor.getValue(); // 캡처한 리스너 꺼내기

        // Redis 메시지 수신 시뮬레이션
        listener.onMessage(
                "topic:auction:bid:*",
                "topic:auction:bid:" + bidInfo.getAuctionId(),
                bidInfo
        );

        verify(messagingTemplate, times(1)).convertAndSend("/topic/auction/" + bidInfo.getAuctionId(), bidInfo);
    }

    /// pub sub 테스트///


}

