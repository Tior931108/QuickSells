package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.deal.entity.Deal;
import com.example.quicksells.domain.deal.repository.DealRepository;
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
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionSettlementServiceTest {

    @Mock private DealRepository dealRepository;
    @Mock private PointLedgerService pointLedgerService;

    @InjectMocks private AuctionSettlementService auctionSettlementService;

    @Test
    @DisplayName("낙찰 정산 실패 - buyer가 null이면 NOT_FOUND_USER")
    void settle_fail_buyerNull() {
        Auction auction = mock(Auction.class);
        when(auction.getBuyer()).thenReturn(null);

        assertThatThrownBy(() -> auctionSettlementService.settleSuccessfulAuction(auction))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.NOT_FOUND_USER.getMessage());
    }

    @Test
    @DisplayName("낙찰 정산 실패 - seller가 null이면 NOT_FOUND_USER")
    void settle_fail_sellerNull() {
        Auction auction = mock(Auction.class);
        User buyer = mock(User.class);
        when(auction.getBuyer()).thenReturn(buyer);

        Appraise appraise = mock(Appraise.class);
        Item item = mock(Item.class);
        when(auction.getAppraise()).thenReturn(appraise);
        when(appraise.getItem()).thenReturn(item);
        when(item.getSeller()).thenReturn(null);

        assertThatThrownBy(() -> auctionSettlementService.settleSuccessfulAuction(auction))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.NOT_FOUND_USER.getMessage());
    }

    @Test
    @DisplayName("낙찰 정산 실패 - bidPrice <= 0 이면 INVALID_CHARGE_AMOUNT")
    void settle_fail_invalidAmount() {
        Auction auction = mock(Auction.class);
        User buyer = mock(User.class);
        when(auction.getBuyer()).thenReturn(buyer);

        Appraise appraise = mock(Appraise.class);
        Item item = mock(Item.class);
        User seller = mock(User.class);

        when(auction.getAppraise()).thenReturn(appraise);
        when(appraise.getItem()).thenReturn(item);
        when(item.getSeller()).thenReturn(seller);

        when(auction.getBidPrice()).thenReturn(0);

        assertThatThrownBy(() -> auctionSettlementService.settleSuccessfulAuction(auction))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.INVALID_CHARGE_AMOUNT.getMessage());
    }

    @Test
    @DisplayName("낙찰 정산 실패 - Deal 없으면 NOT_FOUND_DEAL")
    void settle_fail_notFoundDeal() {
        // given
        Auction auction = mock(Auction.class);

        // buyer null 체크 통과용
        User buyer = mock(User.class);
        when(auction.getBuyer()).thenReturn(buyer);  // buyer 객체 존재만 하면 됨

        // seller null 체크 통과용
        User seller = mock(User.class);

        Appraise appraise = mock(Appraise.class);
        when(appraise.getId()).thenReturn(10L);

        Item item = mock(Item.class);
        when(auction.getAppraise()).thenReturn(appraise);
        when(appraise.getItem()).thenReturn(item);
        when(item.getSeller()).thenReturn(seller);

        // amount 체크 통과용
        when(auction.getBidPrice()).thenReturn(50_000);

        // deal 없음 -> NOT_FOUND_DEAL
        when(dealRepository.findByAppraiseId(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionSettlementService.settleSuccessfulAuction(auction))
                .isInstanceOf(CustomException.class)
                .hasMessage(ExceptionCode.NOT_FOUND_DEAL.getMessage());

        verify(dealRepository).findByAppraiseId(10L);
        verify(pointLedgerService, never()).transfer(anyLong(), anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("낙찰 정산 - 이미 SOLD deal이면 transfer 호출 없이 종료")
    void settle_alreadySold_returns() {
        Auction auction = mock(Auction.class);
        User buyer = mock(User.class);
        when(auction.getBuyer()).thenReturn(buyer);

        User seller = mock(User.class);

        Appraise appraise = mock(Appraise.class);
        when(appraise.getId()).thenReturn(10L);
        Item item = mock(Item.class);

        when(auction.getAppraise()).thenReturn(appraise);
        when(appraise.getItem()).thenReturn(item);
        when(item.getSeller()).thenReturn(seller);

        when(auction.getBidPrice()).thenReturn(50_000);

        Deal deal = mock(Deal.class);
        when(deal.getStatus()).thenReturn(StatusType.SOLD);
        when(dealRepository.findByAppraiseId(10L)).thenReturn(Optional.of(deal));

        auctionSettlementService.settleSuccessfulAuction(auction);

        verify(pointLedgerService, never()).transfer(anyLong(), anyLong(), anyLong(), any(), any(), any());
        verify(deal, never()).completeAuction(any());
    }

    @Test
    @DisplayName("낙찰 정산 성공 - buyer -> seller transfer + deal.completeAuction 호출")
    void settle_success_transfer_and_complete() {
        Auction auction = mock(Auction.class);

        User buyer = mock(User.class);
        when(buyer.getId()).thenReturn(3L);
        when(auction.getBuyer()).thenReturn(buyer);

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(2L);

        Appraise appraise = mock(Appraise.class);
        when(appraise.getId()).thenReturn(10L);

        Item item = mock(Item.class);
        when(auction.getAppraise()).thenReturn(appraise);
        when(appraise.getItem()).thenReturn(item);
        when(item.getSeller()).thenReturn(seller);

        when(auction.getBidPrice()).thenReturn(50_000);
        when(auction.getId()).thenReturn(7L);

        Deal deal = mock(Deal.class);
        when(deal.getStatus()).thenReturn(StatusType.ON_SALE);
        when(dealRepository.findByAppraiseId(10L)).thenReturn(Optional.of(deal));

        ArgumentCaptor<TransactionReference> refCaptor = ArgumentCaptor.forClass(TransactionReference.class);

        auctionSettlementService.settleSuccessfulAuction(auction);

        verify(pointLedgerService).transfer(
                eq(3L),
                eq(2L),
                eq(50_000L),
                eq(PointTransactionType.AUCTION_WIN_DEDUCT),
                eq(PointTransactionType.AUCTION_SELLER_CREDIT),
                refCaptor.capture()
        );

        assertThat(refCaptor.getValue().getAuctionId()).isEqualTo(7L);
        verify(deal).completeAuction(eq(50_000));
    }
}
