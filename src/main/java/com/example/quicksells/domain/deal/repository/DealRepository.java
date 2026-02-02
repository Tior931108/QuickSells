package com.example.quicksells.domain.deal.repository;

import com.example.quicksells.domain.deal.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
