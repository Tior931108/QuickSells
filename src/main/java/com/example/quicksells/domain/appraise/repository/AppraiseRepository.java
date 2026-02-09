package com.example.quicksells.domain.appraise.repository;

import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AppraiseRepository extends JpaRepository<Appraise, Long>, AppraiseCustomRepository {

    // N + 1 문제 해결 - FETCH JOIN 사용
    // JOIN FETCH: user, item은 필수 (INNER JOIN)
    // LEFT JOIN FETCH: deal은 null 가능 (감정 생성 시 or 선택 전)

    // 특정 상품에 이미 선택된 감정이 있는지 확인 - count 쿼리는 JOIN FETCH 불필요
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appraise a WHERE a.item.id = :itemId AND a.isSelected = true")
    boolean existsByItemIdAndIsSelectedTrue(@Param("itemId") Long itemId);

    // 특정 상품에 대해 특정 감정사가 이미 감정을 등록했는지 확인
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appraise a WHERE a.item.id = :itemId AND a.admin.id = :adminId")
    boolean existsByItemIdAndUserId(@Param("itemId") Long itemId, @Param("adminId") Long adminId);

    // 상품마다 감정된 목록 조회
    @Query("SELECT a FROM Appraise a JOIN FETCH a.item WHERE a.item = :item")
    Optional<Appraise> findByItemAppraise(@Param("item") Item item);

    // 상품과 함께
    @Query("SELECT a FROM Appraise a JOIN FETCH a.item WHERE a.id = :appraiseId")
    Optional<Appraise> findByIdWithItem(@Param("appraiseId") Long appraiseId);

    // 특정 상품에 대한 특정 감정사의 감정 조회
    @Query("SELECT a FROM Appraise a JOIN FETCH a.admin JOIN FETCH a.item WHERE a.item = :item AND a.admin.id = :userId")
    Optional<Appraise> findByItemAndUserId(@Param("item") Item item, @Param("userId") Long userId);
}
