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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppraiseService {

    private final AppraiseRepository appraiseRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final AuctionService auctionService;
    private final DealService dealService;
    private final ImmediateSellSettlementService immediateSellSettlementService;

    /**
     * 감정 생성 (관리자 권한만 가능)
     * - Deal은 나중에 판매자가 감정을 선택할 때 생성
     */
    @Transactional
    public AppraiseCreateResponse createAppraise(Long itemId, AppraiseCreateRequest request, AuthUser authUser) {

        // 1. 상품 존재 여부 확인
        Item item = getItem(itemId);

        // 2. 상품이 감정 가능한 상태인지 확인
        validateItemStatus(item);

        // 3. 관리자가 해당 상품으로 이미 감정 생성 했는지 검증
        if (appraiseRepository.existsByItemIdAndUserId(itemId, authUser.getId())) {
            throw new CustomException(ExceptionCode.ALREADY_EXISTS_APPRAISE);
        }

        // 4. 감정사 정보 조회
        User admin = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISER));


        // 5. 감정 엔티티 생성
        Appraise appraise = new Appraise(
                admin,                      // user (감정사)
                item,                       // item
                request.getBidPrice(),      // bidPrice
                false                       // isSelected (기본값)
        );

        // 6. 저장
        Appraise savedAppraise = appraiseRepository.save(appraise);

        return AppraiseCreateResponse.from(savedAppraise);
    }


    /**
     * 상품 상태 검증
     */
    private void validateItemStatus(Item item) {

        // 상품이 삭제된 경우
        if (item.isDeleted()) {
            throw new CustomException(ExceptionCode.NOT_APPRAISE_ITEM_DELETE);
        }
    }

    /**
     * 상품의 감정 목록 전체 조회 (페이징)
     * USER(판매자) : 본인 상품만 조회 가능
     * ADMIN: 모든 상품 조회 가능
     */
    @Transactional(readOnly = true)
    public Page<AppraiseGetAllResponse> getAppraisesByItemId(Long itemId, Pageable pageable, AuthUser authUser) {

        // 1. 상품 존재 여부 확인
        Item item = getItem(itemId);

        // 2. 권한 확인: ADMIN이 아닌 경우 본인 상품인지 검증
        if (!isAdmin(authUser)) {
            validateItemOwner(item, authUser.getId());
        }

        // 3. 감정 목록 조회 (페이징)
        Page<Appraise> appraisePage = appraiseRepository.findByItemIdWithPaging(itemId, pageable);

        // 4. 상품별 감정 목록이 비어 있는 경우
        if (!appraisePage.hasContent()) {
            throw new CustomException(ExceptionCode.NOT_FOUND_APPRAISE);
        }

        // 5. DTO 변환
        return appraisePage.map(AppraiseGetAllResponse::from);
    }

    /**
     * 관리자 본인이 감정한 상품 목록 전체 조회
     */
    @Transactional(readOnly = true)
    public Page<AppraiseAdminGetAllResponse> getMyAdminAppraises(Long appraiserId, AppraiseStatus status, Pageable pageable) {

        Page<Appraise> appraises = appraiseRepository.findByAppraiserIdWithItemAndSeller(appraiserId, status, pageable);

        // 관리자가 감정한 목록이 없을경우
        if(!appraises.hasContent()) {
            throw new CustomException(ExceptionCode.NOT_FOUND_APPRAISE);
        }

        return appraises.map(AppraiseAdminGetAllResponse::from);
    }

    /**
     * 감정 단건 조회
     * USER(판매자) : 본인 상품만 조회 가능
     * ADMIN : 모든 상품 조회 가능
     */
    @Transactional(readOnly = true)
    public AppraiseGetResponse getAppraise(Long id, Long itemId, AuthUser authUser) {

        // 1. 상품 존재 여부 확인
        Item item = getItem(itemId);

        // 2. 권한 확인: ADMIN이 아닌 경우 본인 상품인지 검증
        if (!isAdmin(authUser)) {
            validateItemOwner(item, authUser.getId());
        }

        // 3. 감정 조회
        Appraise appraise = appraiseRepository.findByIdAndItemId(id, itemId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISE));

        // 4. DTO 변환
        return AppraiseGetResponse.from(appraise);
    }

    /**
     * 상품 존재 여부 확인
     */
    private Item getItem(Long itemId) {

        return itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ITEM));
    }

    /**
     * 상품 소유자 검증
     */
    private void validateItemOwner(Item item, Long userId) {

        if (!item.getSeller().getId().equals(userId)) {
            throw new CustomException(ExceptionCode.ONLY_OWNER_APPRAISE_SEARCH);
        }
    }

    /**
     * ADMIN 권한 확인
     */
    private boolean isAdmin(AuthUser authUser) {
        return authUser.getRole() == UserRole.ADMIN;
    }

    /**
     * 관리자 본인이 감정한 상품 상세 조회
     */
    @Transactional(readOnly = true)
    public AppraiseAdminGetResponse getMyAdminAppraiseDetail(Long appraiseId, Long appraiserId) {

        Appraise appraise = appraiseRepository.findByIdWithItemAndSeller(appraiseId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISE));

        // 본인이 감정한 것인지 확인
        validateAppraiser(appraise, appraiserId);

        return AppraiseAdminGetResponse.from(appraise);
    }

    /**
     * 감정 선택 (판매자가 원하는 감정가 선택만 처리)
     */
    @Transactional
    public AppraiseUpdateResponse updateAppraise(Long id, AppraiseUpdateRequest request, AuthUser authUser) {

        // 1. 감정 조회
        Appraise appraise = appraiseRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISE));

        // 2. 권한 검증: 본인 상품의 감정인지 확인
        Item item = appraise.getItem();
        validateItemOwner(item, authUser.getId());

        // 3. 감정 검증 상태인지 확인
        validateAppraise(appraise, item);

        // 4. 선택 여부만 업데이트
        if (request.getIsSelected()) {
            // 기존에 선택된 다른 감정이 있다면 해제 후 선택
            resetOtherSelectedAppraises(item, appraise.getId());
            appraise.updateSelected(request.getIsSelected());
        }

        appraiseRepository.flush();

        return AppraiseUpdateResponse.from(appraise);
    }

    /**
     * 기존에 선택된 다른 감정들 선택 해제
     */
    private void resetOtherSelectedAppraises(Item item, Long currentAppraiseId) {

        List<Appraise> appraises = appraiseRepository.findAllByItem(item);

        appraises.stream()
                .filter(a -> !a.getId().equals(currentAppraiseId))
                .filter(Appraise::isSelected)
                .forEach(a -> a.updateSelected(false));
    }

    /**
     * 즉시 판매 확정
     * - 판매자가 선택한 감정가로 즉시 판매 진행
     */
    @Transactional
    public AppraiseImmediateSellResponse confirmImmediateSell(Long appraiseId) {

        // 1. 감정 조회 (Item도 함께 fetch join으로 가져오기)
        Appraise appraise = appraiseRepository.findByIdWithItem(appraiseId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISE));

        // 2. 비즈니스 검증
        validateAppraiseForProcessing(appraise);

        // 3. 즉시 판매 처리
        appraise.updateStatus(AppraiseStatus.IMMEDIATE_SELL);
        appraise.updateSelected(true);

        Deal deal = dealService.createAppraiseDeal(appraise);

         // 즉시판매 확정이면 회사가 매입 → 판매자에게 포인트 지급
         // 구매자 유저는 존재하지 않음(회사)
        immediateSellSettlementService.creditSeller(appraise, deal);

        // 포인트 지급이 끝난 뒤 거래 완료(SOLD)
        dealService.completeImmediateSellDeal(deal);

        return AppraiseImmediateSellResponse.from(appraise, deal);
    }

    /**
     * 경매 진행 확정 및 생성
     * - 판매자가 선택한 감정가로 경매 확정 및 생성
     */
    @Transactional
    public AppraiseAuctionProceedResponse confirmAuctionWithCreate(Long appraiseId, Integer timeOption) {

        // 1. 감정 조회 및 검증
        Appraise appraise = appraiseRepository.findById(appraiseId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISE));

        validateAppraiseForProcessing(appraise);

        // 2. 경매 진행 상태로 변경
        appraise.updateStatus(AppraiseStatus.AUCTION);
        appraiseRepository.save(appraise);

        // 3. 경매 생성 API
        AuctionCreateRequest auctionRequest = new AuctionCreateRequest(appraiseId, timeOption);
        AuctionCreateResponse auctionResponse = auctionService.saveAuction(auctionRequest);

        // 4. 응답 생성 (경매 정보 포함)
        return AppraiseAuctionProceedResponse.of(appraise, auctionResponse);
    }

    /**
     * 감정 처리 전 비즈니스 검증
     * - 선택 감정인지 확인
     * - 이미 처리된 감정인지 확인
     */
    private void validateAppraiseForProcessing(Appraise appraise) {

        // 선택된 감정인지 확인
        if (!appraise.isSelected()) {
            throw new CustomException(ExceptionCode.APPRAISE_NOT_SELECTED);
        }

        // 이미 처리된 감정인지 확인
        if (appraise.getAppraiseStatus() == AppraiseStatus.IMMEDIATE_SELL ||
                appraise.getAppraiseStatus() == AppraiseStatus.AUCTION) {
            throw new CustomException(ExceptionCode.APPRAISE_ALREADY_PROCESSED);
        }
    }

    /**
     * 감정 선택 가능한지 검증하는 메소드
     */
    private void validateAppraise(Appraise appraise, Item item) {

        // 이미 선택된 감정인지 확인 - (선택한 감정을 다시 선택시)
        if (appraise.isSelected()) {
            throw new CustomException(ExceptionCode.ALREADY_SELECT_APPRAISE);
        }

        // 해당 상품에 이미 다른 감정이 선택되었는지 확인
        if (appraiseRepository.existsByItemIdAndIsSelectedTrue(item.getId())) {
            throw new CustomException(ExceptionCode.EXISTS_ALREADY_SELECT_APPRAISE);
        }
    }

    /**
     * 관리자 본인이 감정한 감정가 수정
     */
    @Transactional
    public AppraiseAdminUpdateResponse updateMyAdminAppraise(Long appraiseId, AppraiseAdminUpdateRequest request, Long appraiserId) {

        // 1. 감정 조회
        Appraise appraise = appraiseRepository.findById(appraiseId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISE));

        // 2. 본인이 감정한 것인지 확인
        validateAppraiser(appraise, appraiserId);

        // 3. 수정 가능 여부 확인
        validateUpdatable(appraise);

        // 4. 감정가 수정
        appraise.updateBidPrice(request.getBidPrice());

        appraiseRepository.save(appraise);

        return AppraiseAdminUpdateResponse.from(appraise);
    }

    /**
     * 본인이 감정한 것인지 확인
     */
    private void validateAppraiser(Appraise appraise, Long appraiserId) {

        if (!appraise.getAdmin().getId().equals(appraiserId)) {
            throw new CustomException(ExceptionCode.FORBIDDEN_APPRAISE_ACCESS);
        }
    }

    /**
     * 수정 가능 여부 확인
     * - 이미 선택되었거나 즉시판매/경매진행 상태면 수정 불가
     */
    private void validateUpdatable(Appraise appraise) {

        if (appraise.isSelected()) {
            throw new CustomException(ExceptionCode.CANNOT_UPDATE_SELECTED_APPRAISE);
        }

        if (appraise.getAppraiseStatus() == AppraiseStatus.IMMEDIATE_SELL ||
                appraise.getAppraiseStatus() == AppraiseStatus.AUCTION) {
            throw new CustomException(ExceptionCode.CANNOT_UPDATE_PROCESSED_APPRAISE);
        }
    }


    /**
     * 감정 삭제 (감정사 ADMIN만 가능)
     * - Soft Delete
     * - 본인이 작성한 감정만 삭제 가능
     * - 선택된 감정(isSelected = true)은 삭제 불가
     */
    @Transactional
    public void deleteAppraise(Long itemId, AuthUser authUser) {

        // 1. 상품 존재 여부 확인
        Item item = getItem(itemId);

        // 2. 해당 상품에 대한 현재 감정사의 감정 조회
        Appraise appraise = appraiseRepository.findByItemAndUserId(item, authUser.getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISE));

        // 3. 이미 선택된 감정인지 확인 (선택된 감정은 삭제 불가)
        if (appraise.isSelected()) {
            throw new CustomException(ExceptionCode.NOT_DELETE_SELECTED_APPRAISE);
        }

        // 4. Soft Delete 처리
        appraise.delete();
    }
}
