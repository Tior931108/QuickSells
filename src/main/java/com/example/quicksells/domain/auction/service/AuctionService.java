package com.example.quicksells.domain.auction.service;

import com.example.quicksells.common.annotation.RedissonLock;
import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.appraise.repository.AppraiseRepository;
import com.example.quicksells.domain.auction.model.dto.BidInfo;
import com.example.quicksells.domain.auction.model.request.AuctionCreateRequest;
import com.example.quicksells.domain.auction.model.request.AuctionSearchFilterRequest;
import com.example.quicksells.domain.auction.model.request.AuctionUpdateRequest;
import com.example.quicksells.domain.auction.model.response.AuctionCreateResponse;
import com.example.quicksells.domain.auction.model.response.AuctionGetAllResponse;
import com.example.quicksells.domain.auction.model.response.AuctionGetResponse;
import com.example.quicksells.domain.auction.model.response.AuctionUpdateResponse;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auction.repository.AuctionRepository;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.deal.service.DealService;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.service.PointWalletService;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AppraiseRepository appraiseRepository;
    private final UserRepository userRepository;
    private final DealService dealService;
    private final ApplicationEventPublisher eventPublisher;
    private final PointWalletService pointWalletService;

    @Transactional
    public AuctionCreateResponse saveAuction(AuctionCreateRequest request) {

        // 감정 조회
        Appraise foundAppraise = appraiseRepository.findById(request.getAppraiseId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_APPRAISE));

        // 중복 검증
        deduplicationAuction(foundAppraise);

        // 경매 생성
        Auction newAuction = new Auction(foundAppraise, foundAppraise.getBidPrice());

        // 경매의 생성일과 경매 종료일 설정
        newAuction.auctionCloseTime(request.getTimeOption());

        Auction saveAuction = auctionRepository.save(newAuction);

        // 2. 거래 생성 (Auction 포함)
        dealService.createAuctionDeal(foundAppraise, saveAuction);

        return AuctionCreateResponse.from(saveAuction);
    }


    @Transactional(readOnly = true)
    public Page<AuctionGetAllResponse> getAllAuction(Pageable pageable, AuctionSearchFilterRequest request) {

        // 경매 페이지 쿼리 dsl 조회
        Page<Auction> foundAuctionPage = auctionRepository.auctionSearch(pageable, request);

        return foundAuctionPage.map(AuctionGetAllResponse::from);
    }

    @Transactional(readOnly = true)
    public AuctionGetResponse getAuction(Long auctionId) {

        // 경매 상세 조회
        Auction foundAuction = auctionRepository.findByIdAndStatusAndEndTimeAfter(auctionId, AuctionStatusType.AUCTIONING, LocalDateTime.now(Clock.systemDefaultZone()))
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_AUCTION));

        return AuctionGetResponse.from(foundAuction);
    }

    @RedissonLock
    @Transactional
    public AuctionUpdateResponse updateBidPrice(Long auctionId, AuctionUpdateRequest request, AuthUser authUser) {

        // 경매 조회
        Auction foundAuction = auctionRepository.findByIdAndStatusAndEndTimeAfter(auctionId, AuctionStatusType.AUCTIONING, LocalDateTime.now(Clock.systemDefaultZone()))
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_AUCTION));

        // 구매자 조회
        User foundBuyer = userRepository.findById(request.getBuyerId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        // 구매자 검증
        validateUser(authUser, foundAuction, foundBuyer);

        // 입찰 가격 검증
        validateBidPrice(request.getBidPrice(), foundAuction.getBidPrice());

        // 경매 입찰
        foundAuction.update(foundBuyer, request.getBidPrice());

        // 경매 정보
        BidInfo bidInfo = new BidInfo(
                foundAuction.getId(),
                foundBuyer.getName(),
                foundAuction.getBidPrice()
        );

        // 잔액 검증(포인트 차감은 안됨)
        PointWallet buyerWallet = pointWalletService.getOrCreate(foundBuyer.getId());
        if (buyerWallet.getAvailableBalance() < request.getBidPrice()) {
            throw new CustomException(ExceptionCode.INSUFFICIENT_BALANCE);
        }

        // 이벤트 퍼블리싱
        eventPublisher.publishEvent(bidInfo);

        return AuctionUpdateResponse.from(foundAuction);
    }


    /**
     * 종료된 경매는 삭제요청 불가
     */
    @Transactional
    public void deleteAuction(Long auctionId) {

        // 경매 조회
        Auction foundAuction = auctionRepository.findByIdAndIsDeletedFalse(auctionId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_AUCTION));

        // 삭제되지 않은 경매 삭제
        foundAuction.auctionDelete();
    }


    /**
     * 검증 메서드
     */

    private void validateBidPrice(Integer requestBidPrice, Integer auctionBidPrice) {

        // 요청 입찰가가 경매 입찰가보다 같거나 낮으면 예외
        if (requestBidPrice <= auctionBidPrice) {
            throw new CustomException(ExceptionCode.BID_PRICE_TOO_LOW);
        }
    }

    private void validateUser(AuthUser authUser, Auction foundAuction, User foundBuyer) {

        Long authUserId = authUser.getId(); // 인증유저 아이디
        Long sellerId = foundAuction.getAppraise().getItem().getSeller().getId(); // 경매에 등록된 감정의 아이템을 등록한 판매자의 아이디
        Long buyerId = foundBuyer.getId(); // 조회된 유저 아이디

        // 인증유저와 구매자가 다를때 예외
        if (!authUserId.equals(buyerId)) {
            throw new CustomException(ExceptionCode.ACCESS_DENIED_ONLY_OWNER);
        }

        // 인증유저와 구매자가 같지만 해당경매의 판매자일때 예외
        if (authUser.getId().equals(sellerId)) {
            throw new CustomException(ExceptionCode.SELLER_CANNOT_PURCHASE_OWN_AUCTION);
        }
    }

    private void deduplicationAuction(Appraise foundAppraise) {

        boolean duplicatedAppraise = auctionRepository.existsByAppraise(foundAppraise);

        // 경매 등록시 기존 경매에 감정 or 거래가 존재하면 중복
        if (duplicatedAppraise) {
            throw new CustomException(ExceptionCode.CONFLICT_AUCTION);
        }
    }

}
