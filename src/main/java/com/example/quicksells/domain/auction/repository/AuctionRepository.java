package com.example.quicksells.domain.auction.repository;

import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.auction.entity.Auction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long>, AuctionCustomRepository {

    boolean existsByAppraise(Appraise appraise);

    // 진행중인 경매에 마감 시간이 현재 시간보다 이 전일떄
    Slice<Auction> findAllByStatusAndEndTimeBefore(Pageable pageable, AuctionStatusType status, LocalDateTime endTime);

    /**
     * 요청한 경매의 진행 상태와 마감 시간이 현재 시간보다 어 후일때
     * 경매는 경매 아이디와 경매 아이템을 응답함
     * N + 1 방지
     */
    @Query("SELECT a FROM Auction a JOIN FETCH a.appraise ap JOIN FETCH ap.item i LEFT JOIN FETCH a.buyer b WHERE a.id = :id AND a.status = :status AND a.endTime > :now")
    Optional<Auction> findByIdAndStatusAndEndTimeAfter(@Param("id") Long id, @Param("status") AuctionStatusType status, @Param("now")LocalDateTime now);

    // 삭제 되지 않은 경매
    Optional<Auction> findByIdAndIsDeletedFalse(Long id);
}
