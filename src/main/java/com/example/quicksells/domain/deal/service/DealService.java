package com.example.quicksells.domain.deal.service;

import com.example.quicksells.common.enums.DealType;
import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.common.enums.UserRole;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepository;
    private final AppraiseRepository appraiseRepository;


    /**
     * 거래 생성 API 비지니스 로직
     * SOLD Deal 존재 -> 새 Deal 생성 가능
     * 막힐 시나리오 : 즉시 판매 후 경매 생성, 경매 생성 후 즉시 판매 생성, 동일 Item로 Deal 2번 생성
     */
    @Transactional
    public DealCreateResponse createDeal(DealCreateRequest request) {

        Appraise appraise = appraiseRepository.findById(request.getAppraiseId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISE));

        // 이미 해당 감정으로 생성된 Deal 존재 여부
        dealRepository.findByAppraiseId(appraise.getId())
                .ifPresent(d -> {
                    throw new CustomException(ExceptionCode.EXISTS_ACTIVE_DEAL);
                });

        Deal deal = new Deal(
                appraise,
                null, // 즉시판매 or 경매 전
                StatusType.ON_SALE,
                request.getDealPrice()
        );

        dealRepository.save(deal);

        return DealCreateResponse.from(deal);
    }

    /**
     * 경매 등록 시 Deal 생성
     * - Auction 생성 직후에 호출되는 구조로 통일(지금 너 코드처럼)
     */
    @Transactional
    public Deal createAuctionDeal(Appraise appraise, Auction auction) {

        dealRepository.findByAppraiseId(appraise.getId())
                .ifPresent(d -> { throw new CustomException(ExceptionCode.EXISTS_ACTIVE_DEAL); });

        Deal deal = new Deal(
                appraise,
                auction,
                StatusType.ON_SALE,
                auction.getBidPrice()
        );

        return dealRepository.save(deal);
    }

    /**
     * 즉시판매 확정 시 Deal 생성 (Appraise 기반)
     * - Auction 없음
     * - Appraise 1:1 Deal
     */
    @Transactional
    public Deal createAppraiseDeal(Appraise appraise) {

        // 이미 Deal 있으면 막거나(정책) / 업데이트 하거나(정책) 둘 중 하나
        dealRepository.findByAppraiseId(appraise.getId())
                .ifPresent(d -> { throw new CustomException(ExceptionCode.EXISTS_ACTIVE_DEAL); });

        Deal deal = new Deal(
                appraise,
                null,
                StatusType.ON_SALE,
                appraise.getBidPrice()
        );

        return dealRepository.save(deal);
    }


    /**
     * 거래 내역 상세 조회 API 비지니스 로직
     * ADMIN, 판매자, 구매자 조회 가능
     * 제 3자 - 403
     * buyer 없는 Deal은 seller만 조회
     * deal없으면 404
     */
    @Transactional(readOnly = true)
    public DealGetResponse getDealDetail(Long dealId, AuthUser authUser) {

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_DEAL));

        // 권한 검증
        validateDealAccess(deal, authUser);

        return DealGetResponse.from(deal);
    }

    /**
     * 쿼리 메서드 적용 전체 조회
     * ADMIN	 모든 거래
     * SELLER	 본인 판매 거래
     * BUYER	 본인 구매 거래
     */
    @Transactional(readOnly = true)
    public Page<DealGetAllQueryResponse> getDeals(DealType type, AuthUser authUser, Pageable pageable) {

        if (isAdmin(authUser)) {
            return dealRepository.findAllDeals(pageable);
        }

        if (type == DealType.PURCHASE) {
            return dealRepository.findPurchaseDeals(authUser.getId(), pageable);
        }

        return dealRepository.findSaleDeals(authUser.getId(), pageable);
    }

    /**
     * 완료 거래 조회 메서드(전 사용자 홈페이지 띄우기 용)
     */
    @Transactional(readOnly = true)
    public List<DealCompletedResponse> getCompletedDeals(int limit) {

        return dealRepository.findCompletedDeals(limit);
    }

    /**
     * 권한 확인 서비스 로직
     */
    private boolean isAdmin(AuthUser authUser) {

        return authUser.getRole() == UserRole.ADMIN;
    }

    /**
     * 거래 상세 조회 시 권한 검증 메서드
     */
    private void validateDealAccess(Deal deal, AuthUser authUser) {

        // ADMIN은 모든 거래 조회 가능
        if (authUser.getRole() == UserRole.ADMIN) {
            return;
        }

        Long userId = authUser.getId();

        boolean isSeller = deal.getAppraise().getItem().getSeller().getId().equals(userId);
        boolean isBuyer = deal.getAuction() != null
                && deal.getAuction().getBuyer() != null
                && deal.getAuction().getBuyer().getId().equals(userId);

        if (!isSeller && !isBuyer) {
            throw new CustomException(ExceptionCode.ACCESS_DENIED_DEAL);
        }
    }

    @Transactional
    public void completeImmediateSellDeal(Deal deal) {
        // 즉시판매 확정 시점에 거래 완료 처리
        deal.updateForAppraise(StatusType.SOLD, deal.getDealPrice());
        dealRepository.save(deal);
    }
}
