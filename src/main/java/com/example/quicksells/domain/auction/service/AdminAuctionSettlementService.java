package com.example.quicksells.domain.auction.service;

import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auction.model.response.AdminAuctionSettlementRetryResponse;
import com.example.quicksells.domain.auction.repository.AuctionRepository;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.deal.repository.DealRepository;
import com.example.quicksells.domain.payment.service.AuctionSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 전용 경매 정산 재시도 서비스
 */
@Service
@RequiredArgsConstructor
public class AdminAuctionSettlementService {

    private final AuctionRepository auctionRepository;
    private final DealRepository dealRepository;
    private final AuctionSettlementService auctionSettlementService;

    /**
     * 낙찰 정산 재시도
     */
    @Transactional
    public AdminAuctionSettlementRetryResponse retrySettlement(Long auctionId) {

        Auction auction = auctionRepository.findByIdAndIsDeletedFalse(auctionId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_AUCTION));

        if (auction.getStatus() != AuctionStatusType.SUCCESSFUL_BID) {
            throw new CustomException(ExceptionCode.NOT_FOUND_AUCTION);
        }

        Deal deal = dealRepository.findByAppraiseId(auction.getAppraise().getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_DEAL));

        // 이미 SOLD면 완료 응답
        if (deal.getStatus() == StatusType.SOLD) {
            return AdminAuctionSettlementRetryResponse.from(auction.getId(), deal.getId());
        }

        try {
            auctionSettlementService.settleSuccessfulAuction(auction); // 낙찰 정산 처리
        } catch (CustomException e) {
            if (e.getExceptionCode() == ExceptionCode.INSUFFICIENT_BALANCE) {
                throw new CustomException(ExceptionCode.AUCTION_SETTLEMENT_PAYMENT_REQUIRED);
            }
            throw e;
        }

        // 정산 성공
        return AdminAuctionSettlementRetryResponse.from(auction.getId(), deal.getId());
    }
}
