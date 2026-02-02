package com.example.quicksells.domain.search.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.search.repository.SearchRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.example.quicksells.domain.search.entity.Search;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor

public class SearchRankingSnapshotService {

    private final SearchRepository searchRepository;


    /**
     * DB에 인기검색 Top10 업데이트
     *
     * @param keyword
     * @param count
     */
    @Transactional
    public void upsertSnapshot(String keyword, Long count) {

        //null 방지
        if (keyword == null) {
            throw new CustomException(ExceptionCode.INVALID_SEARCH_KEYWORD);
        }

        // 검색어 정규화 (앞, 뒤 공백 제거 및 중간에 공백 통일)
        String readKeyword = keyword.trim().replaceAll("\\s+", " ");

        //정규화 후 빈 문자열 검증 (2차 검증)
        if (readKeyword.isEmpty()) {
            throw new CustomException(ExceptionCode.INVALID_SEARCH_KEYWORD);
        }

        // DB 기존 검색어 조회
        Search search = searchRepository.findByKeyword(readKeyword)
                // new Search(readKeyword); -> 추가됨
                .orElseGet(() -> new Search(readKeyword));

        // 조회수 업데이트
        search.updateCount(count);

        // 검색 데이터 저장
        searchRepository.save(search);
    }
}