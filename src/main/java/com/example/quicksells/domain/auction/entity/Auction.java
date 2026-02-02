package com.example.quicksells.domain.auction.entity;

import com.example.quicksells.common.enums.AuctionSettlementStatus;
import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "auctions", indexes = {@Index(name = "idx_auction_status_end_time", columnList = "status, end_time")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 경매 ID

    @OneToOne(fetch = FetchType.LAZY, optional = false) // 감정x -> 경매 등록x
    @JoinColumn(name = "appraise_id", nullable = false)
    private Appraise appraise; // 감정 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer; // 구매자 ID

    @Column(name = "bid_price", nullable = false)
    private Integer bidPrice; // 입찰 확정 가격

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AuctionStatusType status; // 경매 상태

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_deleted")
    private boolean isDeleted; // 삭제 여부

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuctionSettlementStatus settlementStatus = AuctionSettlementStatus.PENDING;

    public Auction(Appraise appraise, Integer bidPrice) {
        this.appraise = appraise;
        this.buyer = null;
        this.bidPrice = bidPrice;
        this.status = AuctionStatusType.AUCTIONING;
        this.isDeleted = false;
    }

    public void auctionCloseTime(int timeOption) {
        this.createdAt = LocalDateTime.now(Clock.systemDefaultZone()); // 서버 기준 시간대
        this.endTime = createdAt.plusDays(timeOption); // timeOption 1~3일 설정가능
    }

    @PreUpdate
    public void auctionUpdateTime() {
        this.updatedAt = LocalDateTime.now(Clock.systemDefaultZone()); // 수정일 적용 후 DB저장
    }

    public void update(User buyer, Integer bidPrice) {
        this.buyer = buyer;
        this.bidPrice = bidPrice;
    }

    public void auctionEndTimeCheck() {

        LocalDateTime nowDate = LocalDateTime.now(Clock.systemDefaultZone()); // 현재 시간

        if (nowDate.isBefore(this.endTime)) {
            return; // 종료 시간 전일때 통과
        }

        if (this.buyer == null) {
            this.status = AuctionStatusType.UNSUCCESSFUL_BID; // 유찰완료 상태 변경
        } else {
            this.status = AuctionStatusType.SUCCESSFUL_BID;// 낙찰완료 상태 변경
        }
    }

    public void auctionDelete() {
        this.status = AuctionStatusType.CANCELED; // 경매취소 상태 변경
        this.isDeleted = true;
    }

    /** 정산 실패 사유(운영/추적용) */
    @Column(length = 300)
    private String settlementFailReason;

    public void markSettlementCompleted() {
        this.settlementStatus = AuctionSettlementStatus.COMPLETED;
        this.settlementFailReason = null;
    }

    public void markPaymentRequired(String reason) {
        this.settlementStatus = AuctionSettlementStatus.PAYMENT_REQUIRED;
        this.settlementFailReason = reason;
    }
}
