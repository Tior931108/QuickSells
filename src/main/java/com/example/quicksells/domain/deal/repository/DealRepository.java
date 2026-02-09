package com.example.quicksells.domain.deal.repository;

import com.example.quicksells.domain.deal.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long>, DealCustomRepository {

    Optional<Deal> findByAppraiseId(Long appraiseId);

    // 채팅방 생성용 조회 (연관 엔티티 Fetch Join)
    @Query("SELECT d FROM Deal d " +
            "JOIN FETCH d.auction a " +
            "JOIN FETCH a.buyer " +
            "JOIN FETCH d.appraise ap " +
            "JOIN FETCH ap.item i " +
            "JOIN FETCH i.seller " +
            "WHERE d.id = :dealId")
    Optional<Deal> findByIdWithUsersForChat(@Param("dealId") Long dealId);

    // 낙찰된 거래 조회 (구매자 기준) - 내가 구매자인 경우의 낙찰된 거래
    @Query("SELECT d FROM Deal d " +
            "JOIN FETCH d.auction a " +
            "JOIN FETCH a.buyer " +
            "JOIN FETCH d.appraise ap " +
            "JOIN FETCH ap.item i " +
            "JOIN FETCH i.seller seller " +
            "WHERE a.buyer.id = :buyerId " +
            "AND ap.appraiseStatus = 'AUCTION' " +
            "AND a.status = 'SUCCESSFUL_BID'")
    List<Deal> findSuccessfulDealsByBuyerId(@Param("buyerId") Long buyerId);

    // 낙찰된 거래 조회 (판매자 기준) - 내가 판매자인 경우의 낙찰된 거래
    @Query("SELECT d FROM Deal d " +
            "JOIN FETCH d.auction a " +
            "JOIN FETCH a.buyer " +
            "JOIN FETCH d.appraise ap " +
            "JOIN FETCH ap.item i " +
            "JOIN FETCH i.seller seller " +
            "WHERE i.seller.id = :sellerId " +
            "AND ap.appraiseStatus = 'AUCTION' " +
            "AND a.status = 'SUCCESSFUL_BID'")
    List<Deal> findSuccessfulDealsBySellerId(@Param("sellerId") Long sellerId);
}
