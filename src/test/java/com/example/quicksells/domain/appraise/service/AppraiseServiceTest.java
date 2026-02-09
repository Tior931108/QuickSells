package com.example.quicksells.domain.appraise.service;

import com.example.quicksells.common.enums.*;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.appraise.model.request.AppraiseAdminUpdateRequest;
import com.example.quicksells.domain.appraise.model.request.AppraiseCreateRequest;
import com.example.quicksells.domain.appraise.model.request.AppraiseUpdateRequest;
import com.example.quicksells.domain.appraise.model.response.*;
import com.example.quicksells.domain.appraise.repository.AppraiseRepository;
import com.example.quicksells.domain.auction.model.request.AuctionCreateRequest;
import com.example.quicksells.domain.auction.model.response.AuctionCreateResponse;
import com.example.quicksells.domain.auction.service.AuctionService;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.deal.service.DealService;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.item.repository.ItemRepository;
import com.example.quicksells.domain.payment.service.ImmediateSellSettlementService;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static com.example.quicksells.common.enums.UserRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AppraiseServiceTest {

    @Mock
    private AppraiseRepository appraiseRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionService auctionService;

    @Mock
    private DealService dealService;

    @Mock
    private ImmediateSellSettlementService immediateSellSettlementService;

    @InjectMocks
    private AppraiseService appraiseService;


    // 공통 테스트 데이터
    private User testAdmin;
    private User testSeller;
    private User testOtherUser;
    private Item testItem;
    private Appraise testAppraise;
    private AuthUser authAdmin;
    private AuthUser authSeller;
    private AuthUser authOtherUser;

    @BeforeEach
    void setUp() {
        // 관리자 생성
        testAdmin = new User("admin@test.com", "password", "admin", UserRole.ADMIN.name());
        ReflectionTestUtils.setField(testAdmin, "id", 1L);

        // 판매자 생성
        testSeller = new User("seller@test.com", "password", "seller", UserRole.USER.name());
        ReflectionTestUtils.setField(testSeller, "id", 2L);

        // 다른 사용자 생성
        testOtherUser = new User("other@test.com", "password", "other", UserRole.USER.name());
        ReflectionTestUtils.setField(testOtherUser, "id", 3L);

        // 테스트 상품 생성
        testItem = new Item(testSeller, "테스트 상품", 50000L, "테스트 상품설명1", "https://placehold.co/600x400");
        ReflectionTestUtils.setField(testItem, "id", 1L);

        // 테스트 감정 생성
        testAppraise = new Appraise(testAdmin, testItem, 100000, false);
        ReflectionTestUtils.setField(testAppraise, "id", 1L);

        // AuthUser 생성
        authAdmin = new AuthUser(1L, "admin@test.com", UserRole.ADMIN, "관리자");
        authSeller = new AuthUser(2L, "seller@test.com", UserRole.USER, "홍길동");
        authOtherUser = new AuthUser(3L, "other@test.com", UserRole.USER, "홍길순");
    }

    // 추가 객체가 필요한 경우를 위한 헬퍼 메소드들
    private User createUser(Long id, String email, String nickname, UserRole role) {
        User user = new User(email, "password", nickname, role.name());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Item createItem(Long id, User seller, String title) {
        Item item = new Item(seller, title, 60000L, "테스트 상품설명2" ,"https://placehold.co/600x400");
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private Appraise createAppraise(Long id, User admin, Item item, Integer bidPrice, boolean isSelected) {
        Appraise appraise = new Appraise(admin, item, bidPrice, isSelected);
        ReflectionTestUtils.setField(appraise, "id", id);
        return appraise;
    }

    private AuthUser createAuthUser(Long id, UserRole role) {
        return new AuthUser(1L, "test@test.com", USER, "홍길동");
    }

    @Test
    @DisplayName("감정 생성 성공 - 관리자가 새로운 감정 생성")
    void createAppraise_Success() {
        // given
        Long itemId = 1L;
        Integer bidPrice = 100000;
        AppraiseCreateRequest request = new AppraiseCreateRequest(bidPrice);
        // 관리자 감정 생성
        Appraise newAppraise = createAppraise(2L, testAdmin, testItem, bidPrice, false);

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.existsByItemIdAndUserId(itemId, 1L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(testAdmin));
        given(appraiseRepository.save(any(Appraise.class))).willReturn(newAppraise);

        // when
        AppraiseCreateResponse response = appraiseService.createAppraise(itemId, request, authAdmin);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getBidPrice()).isEqualTo(bidPrice);
        assertThat(response.getIsSelected()).isFalse();

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).existsByItemIdAndUserId(itemId, 1L);
        verify(userRepository).findById(1L);
        verify(appraiseRepository).save(any(Appraise.class));
    }

    @Test
    @DisplayName("감정 생성 실패 - 상품이 존재하지 않음")
    void createAppraise_Fail_ItemNotFound() {
        // given
        Long itemId = 1L;
        AuthUser authUser = createAuthUser(1L, UserRole.ADMIN);
        AppraiseCreateRequest request = new AppraiseCreateRequest(100000);

        given(itemRepository.findById(itemId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> appraiseService.createAppraise(itemId, request, authUser))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.NOT_FOUND_ITEM.getMessage());

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository, never()).save(any());
    }

    @Test
    @DisplayName("감정 생성 실패 - 이미 해당 관리자가 감정을 생성한 경우")
    void createAppraise_Fail_AlreadyExists() {
        // given
        Long itemId = 1L;
        Long adminId = 1L;
        AppraiseCreateRequest request = new AppraiseCreateRequest(100000);

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.existsByItemIdAndUserId(itemId, adminId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> appraiseService.createAppraise(itemId, request, authAdmin))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.ALREADY_EXISTS_APPRAISE.getMessage());

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).existsByItemIdAndUserId(itemId, adminId);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("감정 생성 실패 - 감정사를 찾을 수 없음")
    void createAppraise_Fail_AppraiserNotFound() {
        // given
        Long itemId = 1L;
        Long adminId = 1L;
        AppraiseCreateRequest request = new AppraiseCreateRequest(100000);

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.existsByItemIdAndUserId(itemId, adminId)).willReturn(false);
        given(userRepository.findById(adminId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> appraiseService.createAppraise(itemId, request, authAdmin))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.NOT_FOUND_APPRAISER.getMessage());

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).existsByItemIdAndUserId(itemId, adminId);
        verify(userRepository).findById(adminId);
    }

    @Test
    @DisplayName("상품의 감정 목록 조회 성공 - 판매자가 본인 상품 조회")
    void getAppraisesByItemId_Success_BySeller() {
        // given
        Long itemId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        User admin2 = createUser(4L, "admin2@test.com", "admin2", UserRole.ADMIN);
        Appraise appraise2 = createAppraise(2L, admin2, testItem, 120000, false);

        Page<Appraise> appraisePage = new PageImpl<>(Arrays.asList(testAppraise, appraise2));

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.findByItemIdWithPaging(itemId, pageable)).willReturn(appraisePage);

        // when
        Page<AppraiseGetAllResponse> result = appraiseService.getAppraisesByItemId(itemId, pageable, authSeller);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getBidPrice()).isEqualTo(100000);

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).findByItemIdWithPaging(itemId, pageable);
    }

    @Test
    @DisplayName("상품의 감정 목록 조회 실패 - 권한 없음 (다른 사용자 상품)")
    void getAppraisesByItemId_Fail_NotOwner() {
        // given
        Long itemId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));

        // when & then
        assertThatThrownBy(() -> appraiseService.getAppraisesByItemId(itemId, pageable, authOtherUser))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.ONLY_OWNER_APPRAISE_SEARCH.getMessage());

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository, never()).findByItemIdWithPaging(any(), any());
    }

    @Test
    @DisplayName("관리자 본인 감정 목록 조회 성공")
    void getMyAdminAppraises_Success() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Appraise> appraisePage = new PageImpl<>(List.of(testAppraise));

        given(appraiseRepository.findByAppraiserIdWithItemAndSeller(adminId, null, pageable))
                .willReturn(appraisePage);

        // when
        Page<AppraiseAdminGetAllResponse> result = appraiseService.getMyAdminAppraises(adminId, null, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(appraiseRepository).findByAppraiserIdWithItemAndSeller(adminId, null, pageable);
    }

    @Test
    @DisplayName("상품의 감정 목록 조회 실패 - 감정 목록이 비어있음")
    void getAppraisesByItemId_Fail_EmptyList() {
        // given
        Long itemId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<Appraise> emptyPage = new PageImpl<>(List.of());

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.findByItemIdWithPaging(itemId, pageable)).willReturn(emptyPage);

        // when & then
        assertThatThrownBy(() -> appraiseService.getAppraisesByItemId(itemId, pageable, authSeller))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.NOT_FOUND_APPRAISE.getMessage());

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).findByItemIdWithPaging(itemId, pageable);
    }

    @Test
    @DisplayName("관리자 본인 감정 목록 조회 실패 - 감정 목록이 없음")
    void getMyAdminAppraises_Fail_EmptyList() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Appraise> emptyPage = new PageImpl<>(List.of());

        given(appraiseRepository.findByAppraiserIdWithItemAndSeller(adminId, null, pageable))
                .willReturn(emptyPage);

        // when & then
        assertThatThrownBy(() -> appraiseService.getMyAdminAppraises(adminId, null, pageable))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.NOT_FOUND_APPRAISE.getMessage());

        verify(appraiseRepository).findByAppraiserIdWithItemAndSeller(adminId, null, pageable);
    }

    @Test
    @DisplayName("감정 단건 조회 성공 - 관리자")
    void getAppraise_Success_ByAdmin() {
        // given
        Long appraiseId = 1L;
        Long itemId = 1L;

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.findByIdAndItemId(appraiseId, itemId)).willReturn(Optional.of(testAppraise));

        // when
        AppraiseGetResponse result = appraiseService.getAppraise(appraiseId, itemId, authAdmin);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getBidPrice()).isEqualTo(100000);

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).findByIdAndItemId(appraiseId, itemId);
    }

    @Test
    @DisplayName("감정 단건 조회 실패 - 감정을 찾을 수 없음")
    void getAppraise_Fail_NotFound() {
        // given
        Long appraiseId = 999L;
        Long itemId = 1L;

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.findByIdAndItemId(appraiseId, itemId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> appraiseService.getAppraise(appraiseId, itemId, authSeller))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.NOT_FOUND_APPRAISE.getMessage());

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).findByIdAndItemId(appraiseId, itemId);
    }

    @Test
    @DisplayName("관리자 감정 상세 조회 실패 - 본인이 감정한 것이 아님")
    void getMyAdminAppraiseDetail_Fail_NotOwner() {
        // given
        Long appraiseId = 1L;
        Long otherAdminId = 999L;

        given(appraiseRepository.findByIdWithItemAndSeller(appraiseId))
                .willReturn(Optional.of(testAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.getMyAdminAppraiseDetail(appraiseId, otherAdminId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.FORBIDDEN_APPRAISE_ACCESS.getMessage());

        verify(appraiseRepository).findByIdWithItemAndSeller(appraiseId);
    }


    @Test
    @DisplayName("감정 선택 성공 - 판매자가 감정 선택")
    void updateAppraise_Success() {
        // given
        Long appraiseId = 1L;
        AppraiseUpdateRequest request = new AppraiseUpdateRequest(true);

        given(appraiseRepository.findById(appraiseId)).willReturn(Optional.of(testAppraise));
        given(appraiseRepository.existsByItemIdAndIsSelectedTrue(1L)).willReturn(false);
        given(appraiseRepository.findByItemAppraise(testItem)).willReturn(Optional.of(testAppraise));

        // when
        AppraiseUpdateResponse result = appraiseService.updateAppraise(appraiseId, request, authSeller);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIsSelected()).isTrue();

        verify(appraiseRepository).findById(appraiseId);
        verify(appraiseRepository).flush();
    }

    @Test
    @DisplayName("감정 선택 실패 - 권한 없음 (다른 사용자의 상품)")
    void updateAppraise_Fail_NotItemOwner() {
        // given
        Long appraiseId = 1L;
        AppraiseUpdateRequest request = new AppraiseUpdateRequest(true);

        given(appraiseRepository.findById(appraiseId)).willReturn(Optional.of(testAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.updateAppraise(appraiseId, request, authOtherUser))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.ONLY_OWNER_APPRAISE_SEARCH.getMessage());

        verify(appraiseRepository).findById(appraiseId);
        verify(appraiseRepository, never()).flush();
    }

    @Test
    @DisplayName("감정 선택 실패 - 이미 선택된 감정")
    void updateAppraise_Fail_AlreadySelected() {
        // given
        Long appraiseId = 1L;
        Appraise selectedAppraise = createAppraise(appraiseId, testAdmin, testItem, 100000, true);
        AppraiseUpdateRequest request = new AppraiseUpdateRequest(true);

        given(appraiseRepository.findById(appraiseId)).willReturn(Optional.of(selectedAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.updateAppraise(appraiseId, request, authSeller))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.ALREADY_SELECT_APPRAISE.getMessage());

        verify(appraiseRepository).findById(appraiseId);
        verify(appraiseRepository, never()).flush();
    }

    @Test
    @DisplayName("즉시 판매 확정 성공")
    void confirmImmediateSell_Success() {
        // given
        Long appraiseId = 1L;
        Appraise selectedAppraise = createAppraise(appraiseId, testAdmin, testItem, 100000, true);

        Deal deal = new Deal(selectedAppraise, null, StatusType.ON_SALE, 100000);
        ReflectionTestUtils.setField(deal, "id", 1L);

        given(appraiseRepository.findByIdWithItem(appraiseId)).willReturn(Optional.of(selectedAppraise));
        given(dealService.createAppraiseDeal(selectedAppraise)).willReturn(deal);
        willDoNothing().given(immediateSellSettlementService).creditSeller(selectedAppraise, deal);
        willDoNothing().given(dealService).completeImmediateSellDeal(deal);

        // when
        AppraiseImmediateSellResponse result = appraiseService.confirmImmediateSell(appraiseId);

        // then
        assertThat(result).isNotNull();
        assertThat(selectedAppraise.getAppraiseStatus()).isEqualTo(AppraiseStatus.IMMEDIATE_SELL);

        verify(appraiseRepository).findByIdWithItem(appraiseId);
        verify(dealService).createAppraiseDeal(selectedAppraise);
        verify(immediateSellSettlementService).creditSeller(selectedAppraise, deal);
        verify(dealService).completeImmediateSellDeal(deal);
    }

    @Test
    @DisplayName("즉시 판매 확정 실패 - 감정을 찾을 수 없음")
    void confirmImmediateSell_Fail_NotFound() {
        // given
        Long appraiseId = 999L;

        given(appraiseRepository.findByIdWithItem(appraiseId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> appraiseService.confirmImmediateSell(appraiseId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.NOT_FOUND_APPRAISE.getMessage());

        verify(appraiseRepository).findByIdWithItem(appraiseId);
    }


    @Test
    @DisplayName("즉시 판매 확정 실패 - 선택되지 않은 감정")
    void confirmImmediateSell_Fail_NotSelected() {
        // given
        Long appraiseId = 1L;

        given(appraiseRepository.findByIdWithItem(appraiseId)).willReturn(Optional.of(testAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.confirmImmediateSell(appraiseId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.APPRAISE_NOT_SELECTED.getMessage());

        verify(appraiseRepository).findByIdWithItem(appraiseId);
        verify(dealService, never()).createAppraiseDeal(any());
    }

    @Test
    @DisplayName("즉시 판매 확정 실패 - 이미 처리된 감정 (즉시판매)")
    void confirmImmediateSell_Fail_AlreadyProcessed_ImmediateSell() {
        // given
        Long appraiseId = 1L;
        Appraise processedAppraise = createAppraise(appraiseId, testAdmin, testItem, 100000, true);
        processedAppraise.updateStatus(AppraiseStatus.IMMEDIATE_SELL);

        given(appraiseRepository.findByIdWithItem(appraiseId)).willReturn(Optional.of(processedAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.confirmImmediateSell(appraiseId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.APPRAISE_ALREADY_PROCESSED.getMessage());

        verify(appraiseRepository).findByIdWithItem(appraiseId);
    }

    @Test
    @DisplayName("경매 진행 확정 성공")
    void confirmAuctionWithCreate_Success() {
        // given
        Long appraiseId = 1L;
        Integer timeOption = 1;
        Appraise selectedAppraise = createAppraise(appraiseId, testAdmin, testItem, 100000, true);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusDays(timeOption);  // 일 단위

        AuctionCreateResponse auctionResponse = new AuctionCreateResponse(1L, appraiseId, 2L, "테스트 상품", 100000, AuctionStatusType.AUCTIONING, now, endTime);

        given(appraiseRepository.findById(appraiseId)).willReturn(Optional.of(selectedAppraise));
        given(appraiseRepository.save(selectedAppraise)).willReturn(selectedAppraise);
        given(auctionService.saveAuction(any(AuctionCreateRequest.class))).willReturn(auctionResponse);

        // when
        AppraiseAuctionProceedResponse result = appraiseService.confirmAuctionWithCreate(appraiseId, timeOption);

        // then
        assertThat(result).isNotNull();
        assertThat(selectedAppraise.getAppraiseStatus()).isEqualTo(AppraiseStatus.AUCTION);

        verify(appraiseRepository).findById(appraiseId);
        verify(appraiseRepository).save(selectedAppraise);
        verify(auctionService).saveAuction(any(AuctionCreateRequest.class));
    }

    @Test
    @DisplayName("경매 진행 확정 실패 - 선택되지 않은 감정")
    void confirmAuctionWithCreate_Fail_NotSelected() {
        // given
        Long appraiseId = 1L;
        Integer timeOption = 1;

        given(appraiseRepository.findById(appraiseId)).willReturn(Optional.of(testAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.confirmAuctionWithCreate(appraiseId, timeOption))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.APPRAISE_NOT_SELECTED.getMessage());

        verify(appraiseRepository).findById(appraiseId);
        verify(auctionService, never()).saveAuction(any());
    }

    @Test
    @DisplayName("경매 진행 확정 실패 - 이미 처리된 감정 (경매)")
    void confirmAuctionWithCreate_Fail_AlreadyProcessed_Auction() {
        // given
        Long appraiseId = 1L;
        Integer timeOption = 1;
        Appraise processedAppraise = createAppraise(appraiseId, testAdmin, testItem, 100000, true);
        processedAppraise.updateStatus(AppraiseStatus.AUCTION);

        given(appraiseRepository.findById(appraiseId)).willReturn(Optional.of(processedAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.confirmAuctionWithCreate(appraiseId, timeOption))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.APPRAISE_ALREADY_PROCESSED.getMessage());

        verify(appraiseRepository).findById(appraiseId);
        verify(auctionService, never()).saveAuction(any());
    }

    @Test
    @DisplayName("관리자 감정가 수정 성공")
    void updateMyAdminAppraise_Success() {
        // given
        Long appraiseId = 1L;
        Long adminId = 1L;
        Integer newBidPrice = 150000;

        AppraiseAdminUpdateRequest request = new AppraiseAdminUpdateRequest(newBidPrice);

        given(appraiseRepository.findById(appraiseId)).willReturn(Optional.of(testAppraise));
        given(appraiseRepository.save(testAppraise)).willReturn(testAppraise);

        // when
        AppraiseAdminUpdateResponse result = appraiseService.updateMyAdminAppraise(appraiseId, request, adminId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getBidPrice()).isEqualTo(newBidPrice);

        verify(appraiseRepository).findById(appraiseId);
        verify(appraiseRepository).save(testAppraise);
    }

    @Test
    @DisplayName("관리자 감정가 수정 실패 - 이미 처리된 감정 (경매진행)")
    void updateMyAdminAppraise_Fail_AlreadyProcessed_Auction() {
        // given
        Long appraiseId = 1L;
        Long adminId = 1L;
        Appraise processedAppraise = createAppraise(appraiseId, testAdmin, testItem, 100000, false);
        processedAppraise.updateStatus(AppraiseStatus.AUCTION);

        AppraiseAdminUpdateRequest request = new AppraiseAdminUpdateRequest(150000);

        given(appraiseRepository.findById(appraiseId)).willReturn(Optional.of(processedAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.updateMyAdminAppraise(appraiseId, request, adminId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.CANNOT_UPDATE_PROCESSED_APPRAISE.getMessage());

        verify(appraiseRepository).findById(appraiseId);
        verify(appraiseRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 감정가 수정 실패 - 이미 선택된 감정")
    void updateMyAdminAppraise_Fail_AlreadySelected() {
        // given
        Long appraiseId = 1L;
        Long adminId = 1L;
        Appraise selectedAppraise = createAppraise(appraiseId, testAdmin, testItem, 100000, true);

        AppraiseAdminUpdateRequest request = new AppraiseAdminUpdateRequest(150000);

        given(appraiseRepository.findById(appraiseId)).willReturn(Optional.of(selectedAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.updateMyAdminAppraise(appraiseId, request, adminId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.CANNOT_UPDATE_SELECTED_APPRAISE.getMessage());

        verify(appraiseRepository).findById(appraiseId);
        verify(appraiseRepository, never()).save(any());
    }

    @Test
    @DisplayName("감정 삭제 성공")
    void deleteAppraise_Success() {
        // given
        Long itemId = 1L;

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.findByItemAndUserId(testItem, 1L)).willReturn(Optional.of(testAppraise));

        // when
        appraiseService.deleteAppraise(itemId, authAdmin);

        // then
        assertThat(testAppraise.isDeleted()).isTrue();

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).findByItemAndUserId(testItem, 1L);
    }

    @Test
    @DisplayName("감정 삭제 실패 - 감정을 찾을 수 없음")
    void deleteAppraise_Fail_NotFound() {
        // given
        Long itemId = 1L;

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.findByItemAndUserId(testItem, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> appraiseService.deleteAppraise(itemId, authAdmin))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.NOT_FOUND_APPRAISE.getMessage());

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).findByItemAndUserId(testItem, 1L);
    }

    @Test
    @DisplayName("감정 삭제 실패 - 선택된 감정은 삭제 불가")
    void deleteAppraise_Fail_SelectedAppraise() {
        // given
        Long itemId = 1L;
        Appraise selectedAppraise = createAppraise(1L, testAdmin, testItem, 100000, true);

        given(itemRepository.findById(itemId)).willReturn(Optional.of(testItem));
        given(appraiseRepository.findByItemAndUserId(testItem, 1L)).willReturn(Optional.of(selectedAppraise));

        // when & then
        assertThatThrownBy(() -> appraiseService.deleteAppraise(itemId, authAdmin))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ExceptionCode.NOT_DELETE_SELECTED_APPRAISE.getMessage());

        verify(itemRepository).findById(itemId);
        verify(appraiseRepository).findByItemAndUserId(testItem, 1L);
        assertThat(selectedAppraise.isDeleted()).isFalse();
    }
}