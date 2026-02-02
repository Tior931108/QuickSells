package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.payment.entity.PointTransaction;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.repository.PointTransactionRepository;
import com.example.quicksells.domain.payment.repository.PointWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 즉시판매(즉시매입) 정산 서비스
 *
 * 정책:
 * - 구매자는 회사(시스템)이므로 buyer 유저는 필요 없음
 * - 판매자(감정->아이템->유저)에게만 포인트 지급
 * - 포인트 지급 내역(PointTransaction)을 남긴다
 */
@Service
@RequiredArgsConstructor
public class ImmediateSellSettlementService {

    private final PointWalletRepository pointWalletRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional
    public void creditSeller(Appraise appraise, Deal deal) {

        Long sellerId = appraise.getItem().getSeller().getId();
        Long amount = appraise.getBidPrice().longValue();

        // 지갑 없으면 생성
        PointWallet sellerWallet = pointWalletRepository.findById(sellerId)
                .orElseGet(() -> pointWalletRepository.save(new PointWallet(sellerId)));

        // 판매자 포인트 지급
        sellerWallet.increaseBalance(amount);
        pointWalletRepository.save(sellerWallet);

        // 거래내역 기록 (dealId로 추적)
        pointTransactionRepository.save(new PointTransaction(
                sellerId,
                PointTransactionType.IMMEDIATE_SELL_CREDIT,
                amount,
                null,
                null,
                deal.getId()
        ));
    }
}
