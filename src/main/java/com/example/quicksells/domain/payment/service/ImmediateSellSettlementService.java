package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.payment.model.TransactionReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 즉시판매 정산(판매자 적립)
 *
 * ※ 실제 정산(지갑+거래내역)은 PointLedgerService가 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ImmediateSellSettlementService {

    private final PointLedgerService pointLedgerService;

    @Transactional
    public void creditSeller(Appraise appraise, Deal deal) {

        Long sellerId = appraise.getItem().getSeller().getId();
        long amount = appraise.getBidPrice().longValue();

        pointLedgerService.credit(
                sellerId,
                amount,
                PointTransactionType.IMMEDIATE_SELL_CREDIT,
                TransactionReference.ofDeal(deal.getId())
        );
    }
}
