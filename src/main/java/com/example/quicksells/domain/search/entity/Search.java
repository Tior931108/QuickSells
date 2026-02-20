package com.example.quicksells.domain.search.entity;

import com.example.quicksells.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "searches")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Search extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String keyword;

    @Column(nullable = false)
    private Long count;


    public Search(String keyword) {
        this.keyword = keyword;
        this.count = 0L; //0부터 카운트 시작
    }

    //카운트 업데이트
    public void updateCount(Long count) {
        this.count = count;
    }
}
