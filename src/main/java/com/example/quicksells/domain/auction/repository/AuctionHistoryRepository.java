package com.example.quicksells.domain.auction.repository;

import com.example.quicksells.domain.auction.entity.AuctionHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionHistoryRepository extends JpaRepository<AuctionHistory, Long> {

    Slice<AuctionHistory> findByBuyerId(Pageable pageable, Long buyerId);
}
