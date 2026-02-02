package com.example.quicksells.domain.search.entity;

import com.example.quicksells.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Table(name = "searches")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Search extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "search_id", nullable = false, unique = true)
    private Long id;

    @Column(name = "keyword", length = 50, nullable = false, unique = true)
    private String keyword;

    @Column(name = "count", nullable = false)
    private Long count;


    public Search(String keyword) {
        this.keyword = keyword;
        this.count = 0L; //0부터 카운트 시작
    }

    //검색어 증가 (+1)
    public void increase() {
        this.count++;
    }

    public void updateCount(Long count) {
        this.count = count;
    }
}
