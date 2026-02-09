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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "deal_price" , nullable = false)
    private Integer dealPrice;

    @Column(name = "created_at" , nullable = false, updatable = false)
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
    public void completeAuction(Integer finalPrice) {
        if (this.status != StatusType.ON_SALE) {
            throw new CustomException(ExceptionCode.NOT_DEAL_ON_SALE);
        }

        // 경매가 완료된 시점의 구매자 처리
        this.dealPrice = finalPrice;
        this.status = StatusType.SOLD;
    }

    // 감정 선택 시 Deal 업데이트
    public void updateForAppraise(StatusType status, Integer dealPrice) {
        this.status = status;
        this.dealPrice = dealPrice;
    }
}
