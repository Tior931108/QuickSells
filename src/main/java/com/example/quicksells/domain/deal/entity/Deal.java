package com.example.quicksells.domain.deal.entity;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "deals", uniqueConstraints = {@UniqueConstraint(columnNames = "appraise_id")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 경매 결과 (즉시판매면 null)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    /**
     * 감정 결과 (필수)
     * Deal은 감정 결과를 기준으로 생성된다
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appraise_id", nullable = false)
    private Appraise appraise;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusType status;

    @Column(nullable = false)
    private Integer dealPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성 시간
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Deal(Appraise appraise, Auction auction, StatusType status, Integer dealPrice) {
        this.appraise = appraise;
        this.auction = auction;
        this.status = status;
        this.dealPrice = dealPrice;
    }


    /* ===== 비즈니스 메서드 ===== */

    // 거래 완료 처리
    public void completeAuction(User buyer, Integer finalPrice) {
        if (this.status != StatusType.ON_SALE) {
            throw new CustomException(ExceptionCode.NOT_DEAL_ON_SALE);
        }

        // 경매가 완료된 시점의 구매자 처리
        this.dealPrice = finalPrice;
        this.status = StatusType.SOLD;

        // 이 부분에서 buyer 또는 seller를 사용하는 다른 로직이 필요할 수 있습니다.
    }

    // 감정 선택 시 Deal 업데이트
    public void updateForAppraise(StatusType status, Integer dealPrice) {
        this.status = status;
        this.dealPrice = dealPrice;
    }

    /**
     * 회사 즉시매입 완료 처리
     *
     * 정책:
     * - 구매자는 시스템(회사)이므로 일반 유저 buyer는 필요 없다.
     * - (선택) buyer 컬럼을 유지하는 경우, 운영자(ADMIN) 계정을 buyer로 저장할 수 있다.
     */
    public void completeInstantSaleByCompany(User adminBuyer) {
        if (this.status != StatusType.ON_SALE) {
            throw new CustomException(ExceptionCode.NOT_DEAL_ON_SALE);
        }

        this.status = StatusType.SOLD;
    }
}
