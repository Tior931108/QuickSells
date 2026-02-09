package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.deal.repository.DealRepository;
import com.example.quicksells.domain.payment.model.TransactionReference;
import com.example.quicksells.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경매 낙찰 정산 서비스
 *
 *  역할
 * - 낙찰 확정 시점에 "돈(포인트)"를 실제로 이동.
 *   buyer(낙찰자) -> seller(판매자) 포인트 이동.
 * - Deal 상태를 SOLD로 변경하여 거래를 완료 처리.
 *
 *  별도 서비스 분리이유 : 기존 도메인 로직을 최대한 이용하고 보수하기 편하게 설계하기 위함.
 * - 경매 종료 체크(스케줄러) 로직과
 * - 정산(포인트 이동/거래 완료) 로직을 분리하면 유지보수 용이하여 분리해 두었음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSettlementService {

    private final DealRepository dealRepository;
    private final PointLedgerService pointLedgerService;

    /**
     * 경매 낙찰 정산 처리
     *
     * @param auction 종료된 경매(낙찰 상태여야 함)
     */
    @Transactional
    public void settleSuccessfulAuction(Auction auction) {

        // 낙찰자 / 판매자 / 정산금액 산출
        User buyer = auction.getBuyer();
        if (buyer == null) {
            // 낙찰 상태인데 buyer가 없다면 데이터 정합성 문제
            throw new CustomException(ExceptionCode.NOT_FOUND_USER);
        }

        User seller = auction.getAppraise().getItem().getSeller();
        if (seller == null) {
            throw new CustomException(ExceptionCode.NOT_FOUND_USER);
        }

        long amount = auction.getBidPrice().longValue();
        if (amount <= 0) {
            throw new CustomException(ExceptionCode.INVALID_CHARGE_AMOUNT);
        }

        // 2) Deal 조회 (경매 생성 시 createAuctionDeal로 만들어둔 Deal 조회)
        Deal deal = dealRepository.findByAppraiseId(auction.getAppraise().getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_DEAL));

        // 이미 완료된 거래면(중복 정산 방지) 그냥 종료
        if (deal.getStatus() == StatusType.SOLD) {
            log.info("[AuctionSettlement] 이미 정산 완료됨. auctionId={}, dealId={}", auction.getId(), deal.getId());
            return;
        }

        /**
         * 4) 포인트 이동
         * - buyer 차감
         * - seller 적립
         *
         *  여기서 buyerWallet.decreaseBalance()는 잔액 부족 시 예외 발생
         * - 예외 처리 이후 재정산 API 호출
         */
        pointLedgerService.transfer(
                buyer.getId(),
                seller.getId(),
                amount,
                PointTransactionType.AUCTION_WIN_DEDUCT,
                PointTransactionType.AUCTION_SELLER_CREDIT,
                TransactionReference.ofAuction(auction.getId())
        );

        // 6) Deal 완료 처리(상태 SOLD + 최종 가격 반영)
        deal.completeAuction(auction.getBidPrice());

        log.info("[AuctionSettlement] 정산 완료. auctionId={}, amount={}, buyerId={}, sellerId={}",
                auction.getId(), amount, buyer.getId(), seller.getId());
    }
}
