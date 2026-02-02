package com.example.quicksells.domain.search.repository;

import com.example.quicksells.domain.search.entity.Search;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SearchRepository extends JpaRepository<Search, Long> {
    /**
     * 검색어(keyword)로 Search 조회
     * @param keyword
     * @return DB에 있으면 Optional 담아서 반환
     * 없으면 Optional.empty() 반환
     */
    Optional<Search> findByKeyword(String keyword);

    /**
     * 스케줄러에서 오래된 검색 로그 정리 시 사용
     * @param oldDate > 이전 시간 내역 삭제
     */
    @Modifying // 수정/삭제 관련 쿼리
    @Query("delete from Search s where s.createdAt < :oldDate")
    void deletedOldLogs(@Param("oldDate") LocalDateTime oldDate);
}
