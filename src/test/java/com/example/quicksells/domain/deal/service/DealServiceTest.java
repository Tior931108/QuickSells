package com.example.quicksells.domain.deal.service;

import com.example.quicksells.common.enums.DealType;
import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.appraise.repository.AppraiseRepository;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.deal.model.request.DealCreateRequest;
import com.example.quicksells.domain.deal.model.response.DealCompletedResponse;
import com.example.quicksells.domain.deal.model.response.DealCreateResponse;
import com.example.quicksells.domain.deal.model.response.DealGetAllQueryResponse;
import com.example.quicksells.domain.deal.model.response.DealGetResponse;
import com.example.quicksells.domain.deal.repository.DealRepository;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static com.example.quicksells.common.enums.UserRole.ADMIN;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {

    @Mock private DealRepository dealRepository;
    @Mock private AppraiseRepository appraiseRepository;

    @InjectMocks private DealService dealService;

    @Test
    @DisplayName("거래 생성 성공 - createDeal() (save 시 id 세팅 포함)")
    void createDeal_success() {
        // given
        DealCreateRequest request = new DealCreateRequest(10L, 30000);

        Appraise appraise = mock(Appraise.class);
        when(appraise.getId()).thenReturn(10L);

        when(appraiseRepository.findById(10L)).thenReturn(Optional.of(appraise));
        when(dealRepository.findByAppraiseId(10L)).thenReturn(Optional.empty());

        // service가 save() 반환값을 안 쓰고 'deal' 객체로 response를 만들기 때문에,
        // JPA처럼 "save 인자로 들어온 객체"에 id를 박아줘야 DealCreateResponse.from(deal)에서 null이 안 나옴.
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> {
            Deal arg = inv.getArgument(0);
            ReflectionTestUtils.setField(arg, "id", 100L);
            return arg;
        });

        // when
        DealCreateResponse response = dealService.createDeal(request);

        // then
        assertThat(response.getDealId()).isEqualTo(100L);
        assertThat(response.getDealPrice()).isEqualTo(30000);
        verify(dealRepository).save(any(Deal.class));
    }

    @Test
    @DisplayName("거래 생성 실패 - 감정 없음(NOT_FOUND_APPRAISE)")
    void createDeal_fail_notFoundAppraise() {
        DealCreateRequest request = new DealCreateRequest(10L, 30000);

        when(appraiseRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dealService.createDeal(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.NOT_FOUND_APPRAISE.getMessage());
    }

    @Test
    @DisplayName("거래 생성 실패 - 이미 거래 존재(EXISTS_ACTIVE_DEAL) (ifPresent throw 분기 커버)")
    void createDeal_fail_existsActiveDeal() {
        DealCreateRequest request = new DealCreateRequest(10L, 30000);

        Appraise appraise = mock(Appraise.class);
        when(appraise.getId()).thenReturn(10L);

        when(appraiseRepository.findById(10L)).thenReturn(Optional.of(appraise));
        when(dealRepository.findByAppraiseId(10L)).thenReturn(Optional.of(mock(Deal.class)));

        assertThatThrownBy(() -> dealService.createDeal(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.EXISTS_ACTIVE_DEAL.getMessage());

        verify(dealRepository, never()).save(any());
    }

    @Test
    @DisplayName("경매 Deal 생성 성공 - createAuctionDeal() (auction 라인 커버)")
    void createAuctionDeal_success() {
        Appraise appraise = mock(Appraise.class);
        when(appraise.getId()).thenReturn(10L);

        Auction auction = mock(Auction.class);
        when(auction.getBidPrice()).thenReturn(45000);

        when(dealRepository.findByAppraiseId(10L)).thenReturn(Optional.empty());
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));

        Deal saved = dealService.createAuctionDeal(appraise, auction);

        assertThat(saved).isNotNull();
        verify(dealRepository).save(any(Deal.class));
    }

    @Test
    @DisplayName("경매 Deal 생성 실패 - 이미 거래 존재(EXISTS_ACTIVE_DEAL) (ifPresent throw 분기 커버)")
    void createAuctionDeal_fail_existsActiveDeal() {
        Appraise appraise = mock(Appraise.class);
        when(appraise.getId()).thenReturn(10L);

        Auction auction = mock(Auction.class);

        when(dealRepository.findByAppraiseId(10L)).thenReturn(Optional.of(mock(Deal.class)));

        assertThatThrownBy(() -> dealService.createAuctionDeal(appraise, auction))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.EXISTS_ACTIVE_DEAL.getMessage());

        verify(dealRepository, never()).save(any());
    }

    @Test
    @DisplayName("즉시판매(감정 기반) Deal 생성 성공 - createAppraiseDeal() (null auction 라인 커버)")
    void createAppraiseDeal_success() {
        Appraise appraise = mock(Appraise.class);
        when(appraise.getId()).thenReturn(10L);
        when(appraise.getBidPrice()).thenReturn(30000);

        when(dealRepository.findByAppraiseId(10L)).thenReturn(Optional.empty());
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));

        Deal saved = dealService.createAppraiseDeal(appraise);

        assertThat(saved).isNotNull();
        verify(dealRepository).save(any(Deal.class));
    }

    @Test
    @DisplayName("즉시판매(감정 기반) Deal 생성 실패 - 이미 거래 존재(EXISTS_ACTIVE_DEAL) (ifPresent throw 분기 커버)")
    void createAppraiseDeal_fail_existsActiveDeal() {
        Appraise appraise = mock(Appraise.class);
        when(appraise.getId()).thenReturn(10L);

        when(dealRepository.findByAppraiseId(10L)).thenReturn(Optional.of(mock(Deal.class)));

        assertThatThrownBy(() -> dealService.createAppraiseDeal(appraise))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.EXISTS_ACTIVE_DEAL.getMessage());

        verify(dealRepository, never()).save(any());
    }

    @Test
    @DisplayName("거래 상세 조회 성공 - ADMIN은 접근 가능")
    void getDealDetail_success_admin() {
        AuthUser adminAuth = new AuthUser(1L, "admin@test.com", ADMIN, "관리자");

        Deal deal = mock(Deal.class);
        Appraise appraise = mock(Appraise.class);

        when(deal.getAppraise()).thenReturn(appraise);
        when(appraise.getAppraiseStatus()).thenReturn(null);
        when(dealRepository.findById(100L)).thenReturn(Optional.of(deal));

        DealGetResponse response = dealService.getDealDetail(100L, adminAuth);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("거래 상세 조회 성공 - 판매자 접근 가능")
    void getDealDetail_success_seller() {
        AuthUser sellerAuth = new AuthUser(2L, "seller@test.com", USER, "판매자");

        Deal deal = mockDealGraph(/*sellerId*/2L, /*buyerId*/3L, /*auctionPresent*/true, /*buyerPresent*/true);

        when(dealRepository.findById(100L)).thenReturn(Optional.of(deal));

        DealGetResponse response = dealService.getDealDetail(100L, sellerAuth);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("거래 상세 조회 성공 - 구매자 접근 가능")
    void getDealDetail_success_buyer() {
        AuthUser buyerAuth = new AuthUser(3L, "buyer@test.com", USER, "구매자");

        Deal deal = mockDealGraph(/*sellerId*/2L, /*buyerId*/3L, /*auctionPresent*/true, /*buyerPresent*/true);
        when(dealRepository.findById(100L)).thenReturn(Optional.of(deal));

        DealGetResponse response = dealService.getDealDetail(100L, buyerAuth);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("거래 상세 조회 실패 - 제3자 접근 불가(ACCESS_DENIED_DEAL)")
    void getDealDetail_fail_forbidden() {
        AuthUser otherAuth = new AuthUser(999L, "other@test.com", USER, "제3자");

        Deal deal = mockDealGraph(/*sellerId*/2L, /*buyerId*/3L, /*auctionPresent*/true, /*buyerPresent*/true);
        when(dealRepository.findById(100L)).thenReturn(Optional.of(deal));

        assertThatThrownBy(() -> dealService.getDealDetail(100L, otherAuth))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.ACCESS_DENIED_DEAL.getMessage());
    }

    @Test
    @DisplayName("거래 상세 조회 - auction은 있지만 buyer가 null이면 구매자 불가, 판매자만 가능 (buyer null branch 커버)")
    void getDealDetail_buyerNull_branch() {
        AuthUser sellerAuth = new AuthUser(2L, "seller@test.com", USER, "판매자");

        Deal deal = mockDealGraph(/*sellerId*/2L, /*buyerId*/3L, /*auctionPresent*/true, /*buyerPresent*/false);
        when(dealRepository.findById(100L)).thenReturn(Optional.of(deal));

        assertThatCode(() -> dealService.getDealDetail(100L, sellerAuth))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("거래 상세 조회 실패 - 거래 없음(NOT_FOUND_DEAL)")
    void getDealDetail_fail_notFoundDeal() {
        AuthUser userAuth = new AuthUser(2L, "user@test.com", USER, "유저");

        when(dealRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dealService.getDealDetail(100L, userAuth))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.NOT_FOUND_DEAL.getMessage());
    }

    @Test
    @DisplayName("거래 목록 조회 - ADMIN이면 전체 조회(findAllDeals)")
    void getDeals_admin_all() {
        AuthUser adminAuth = new AuthUser(1L, "admin@test.com", ADMIN, "관리자");

        PageRequest pageable = PageRequest.of(0, 10);
        Page<DealGetAllQueryResponse> mocked = new PageImpl<>(List.of(), pageable, 0);

        when(dealRepository.findAllDeals(pageable)).thenReturn(mocked);

        dealService.getDeals(DealType.PURCHASE, adminAuth, pageable);

        verify(dealRepository).findAllDeals(pageable);
        verify(dealRepository, never()).findPurchaseDeals(anyLong(), any());
        verify(dealRepository, never()).findSaleDeals(anyLong(), any());
    }

    @Test
    @DisplayName("거래 목록 조회 - PURCHASE면 구매 내역(findPurchaseDeals)")
    void getDeals_purchase() {
        AuthUser userAuth = new AuthUser(2L, "user@test.com", USER, "유저");

        PageRequest pageable = PageRequest.of(0, 10);
        Page<DealGetAllQueryResponse> mocked = new PageImpl<>(List.of(), pageable, 0);

        when(dealRepository.findPurchaseDeals(2L, pageable)).thenReturn(mocked);

        dealService.getDeals(DealType.PURCHASE, userAuth, pageable);

        verify(dealRepository).findPurchaseDeals(2L, pageable);
        verify(dealRepository, never()).findSaleDeals(anyLong(), any());
        verify(dealRepository, never()).findAllDeals(any());
    }

    @Test
    @DisplayName("거래 목록 조회 - SALE이면 판매 내역(findSaleDeals) (else branch 커버)")
    void getDeals_sale_elseBranch() {
        AuthUser userAuth = new AuthUser(2L, "user@test.com", USER, "유저");

        PageRequest pageable = PageRequest.of(0, 10);
        Page<DealGetAllQueryResponse> mocked = new PageImpl<>(List.of(), pageable, 0);

        when(dealRepository.findSaleDeals(2L, pageable)).thenReturn(mocked);

        dealService.getDeals(DealType.SALE, userAuth, pageable);

        verify(dealRepository).findSaleDeals(2L, pageable);
        verify(dealRepository, never()).findPurchaseDeals(anyLong(), any());
        verify(dealRepository, never()).findAllDeals(any());
    }

    @Test
    @DisplayName("완료 거래 조회 - repository.findCompletedDeals(limit) 호출 (라인 커버)")
    void getCompletedDeals_callsRepository() {
        when(dealRepository.findCompletedDeals(10)).thenReturn(List.of());

        List<DealCompletedResponse> res = dealService.getCompletedDeals(10);

        assertThat(res).isNotNull();
        verify(dealRepository).findCompletedDeals(10);
    }

    @Test
    @DisplayName("즉시판매 완료 처리 - updateForAppraise 호출 + save 호출 (라인 커버)")
    void completeImmediateSellDeal_updatesAndSaves() {
        Deal deal = mock(Deal.class);
        when(deal.getDealPrice()).thenReturn(30000);

        dealService.completeImmediateSellDeal(deal);

        verify(deal).getDealPrice();
        verify(deal).updateForAppraise(StatusType.SOLD, 30000);
        verify(dealRepository).save(deal);
    }

    private Deal mockDealGraph(long sellerId, long buyerId, boolean auctionPresent, boolean buyerPresent) {
        Deal deal = mock(Deal.class);

        // deal.getAppraise().getItem().getSeller().getId()
        Appraise appraise = mock(Appraise.class);
        Item item = mock(Item.class);
        User seller = mock(User.class);

        when(deal.getAppraise()).thenReturn(appraise);
        when(appraise.getItem()).thenReturn(item);
        when(item.getSeller()).thenReturn(seller);
        when(seller.getId()).thenReturn(sellerId);

        if (!auctionPresent) {
            when(deal.getAuction()).thenReturn(null);
            return deal;
        }

        Auction auction = mock(Auction.class);
        when(deal.getAuction()).thenReturn(auction);

        if (!buyerPresent) {
            when(auction.getBuyer()).thenReturn(null);
            return deal;
        }

        User buyer = mock(User.class);
        when(auction.getBuyer()).thenReturn(buyer);
        when(buyer.getId()).thenReturn(buyerId);

        return deal;
    }
}
