package com.example.quicksells.domain.wishlist.entity;

import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "wish_list", uniqueConstraints = @UniqueConstraint(columnNames = {"buyer_id", "auction_id"})) // 중복 저장 x
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public WishList(User buyer, Auction auction) {
        this.buyer = buyer;
        this.auction = auction;
    }

    @PrePersist
    public void CreatedAt() {
        this.createdAt = LocalDateTime.now(Clock.systemDefaultZone());
    }
}
