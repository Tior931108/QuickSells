package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.payment.model.TransactionReference;
import com.example.quicksells.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImmediateSellSettlementServiceTest {

    @Mock private PointLedgerService pointLedgerService;

    @InjectMocks private ImmediateSellSettlementService immediateSellSettlementService;

    @Test
    @DisplayName("즉시판매 정산 - 판매자에게 credit 호출 + dealId 참조 포함")
    void creditSeller_callsLedgerCredit() {
        Appraise appraise = mock(Appraise.class);
        Item item = mock(Item.class);
        User seller = mock(User.class);

        when(appraise.getItem()).thenReturn(item);
        when(item.getSeller()).thenReturn(seller);
        when(seller.getId()).thenReturn(2L);
        when(appraise.getBidPrice()).thenReturn(30_000);

        Deal deal = mock(Deal.class);
        when(deal.getId()).thenReturn(100L);

        ArgumentCaptor<TransactionReference> refCaptor = ArgumentCaptor.forClass(TransactionReference.class);

        immediateSellSettlementService.creditSeller(appraise, deal);

        verify(pointLedgerService).credit(
                eq(2L),
                eq(30_000L),
                eq(PointTransactionType.IMMEDIATE_SELL_CREDIT),
                refCaptor.capture()
        );

        assertThat(refCaptor.getValue().getDealId()).isEqualTo(100L);
        assertThat(refCaptor.getValue().getPaymentId()).isNull();
        assertThat(refCaptor.getValue().getAuctionId()).isNull();
    }
}
