package com.example.quicksells.domain.appraise.entity;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import org.hibernate.annotations.SQLRestriction;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Table(name = "appraises")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Appraise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 감정 고유 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id")
    private User admin; // 감정사 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item; // 상품 ID

    @Enumerated(EnumType.STRING)
    @Column(name = "appraise_status", nullable = false, length = 20)
    private AppraiseStatus appraiseStatus;

    @Column(name = "bid_price", nullable = false)
    private Integer bidPrice; // 감정 가격

    @Column(name = "is_selected", nullable = false)
    private boolean isSelected; // 구매자 선택여부 (false > 경매 돌임, true > 즉시 매임)

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted; // 삭제 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 시간

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Appraise(User admin, Item item, Integer bidPrice, boolean isSelected) {
        this.admin = admin;
        this.item = item;
        this.appraiseStatus = AppraiseStatus.PENDING; // 처음 감정 생성시 : 대기중
        this.bidPrice = bidPrice;
        this.isSelected = isSelected;
        this.isDeleted = false;
    }

    public void updateSelected(boolean isSelected) { this.isSelected = isSelected; }

    // 감정 진행 상태 업데이트
    public void updateStatus(AppraiseStatus status) {
        this.appraiseStatus = status;
    }

    // 감정가 업데이트
    public void updateBidPrice(Integer bidPrice) {
        this.bidPrice = bidPrice;
    }

    // 감정 삭제 처리
    public void delete() {
        this.isDeleted = true;
    }


}
