package com.example.quicksells.domain.auction.entity;

import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "auction_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @Column(name = "bid_price")
    private Integer bidPrice;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AuctionHistory(Auction auction, User buyer, Integer bidPrice, LocalDateTime updatedAt) {
        this.auction = auction;
        this.buyer = buyer;
        this.bidPrice = bidPrice;
        this.updatedAt = updatedAt;
    }

}
