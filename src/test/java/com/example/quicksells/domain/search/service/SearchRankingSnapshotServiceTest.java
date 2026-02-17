package com.example.quicksells.domain.search.service;

import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.search.entity.Search;
import com.example.quicksells.domain.search.repository.SearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchRankingSnapshotServiceTest {

    @Mock
    private SearchRepository searchRepository;

    @InjectMocks
    private SearchRankingSnapshotService searchRankingSnapshotService;


    @Test
    @DisplayName("upsertSnapshot keyword null예외")
    void upsertSnapshot_ketword_null() {
        assertThrows(CustomException.class, () ->
                searchRankingSnapshotService.upsertSnapshot(null, 10L)
        );
        verifyNoInteractions(searchRepository);
    }

    @Test
    @DisplayName("upsertSnapshot keyword empty")
    void upsertSnapshot_keyword_empty() {
        // When & Then
        assertThrows(CustomException.class, () ->
                searchRankingSnapshotService.upsertSnapshot("   ", 10L)
        );

        // 저장 안 됨
        verifyNoInteractions(searchRepository);
    }

    @Test
    @DisplayName("upsertSnapshot - 기존 검색어 있으면 업데이트")
    void upsertSnapshot_existing_keyword() {
        // Given
        String keyword = "아이폰";
        Long count = 10L;

        Search existingSearch = mock(Search.class);

        // 기존 검색어가 있음
        when(searchRepository.findByKeyword(eq(keyword)))
                .thenReturn(Optional.of(existingSearch));

        // When
        searchRankingSnapshotService.upsertSnapshot(keyword, count);

        // Then
        verify(existingSearch).updateCount(eq(count));  // 업데이트 호출 확인
        verify(searchRepository).save(eq(existingSearch));  // 저장 호출 확인
    }

    @Test
    @DisplayName("upsertSnapshot - 기존 검색어 없으면 새로 생성")
    void upsertSnapshot_new_keyword() {
        // Given
        String keyword = "맥북";
        Long count = 5L;

        // 기존 검색어 없음
        when(searchRepository.findByKeyword(eq(keyword)))
                .thenReturn(Optional.empty());

        // When
        searchRankingSnapshotService.upsertSnapshot(keyword, count);

        // Then
        verify(searchRepository).save(any(Search.class));  // 새로 저장 호출 확인
    }

    @Test
    @DisplayName("upsertSnapshot - 앞뒤 공백 제거 후 정상 처리")
    void upsertSnapshot_trim_keyword() {
        // Given
        String keyword = "  아이폰  ";  // 공백 포함
        String trimmedKeyword = "아이폰";  // trim 결과
        Long count = 10L;

        Search existingSearch = mock(Search.class);

        // trim된 키워드로 조회
        when(searchRepository.findByKeyword(eq(trimmedKeyword)))
                .thenReturn(Optional.of(existingSearch));

        // When
        searchRankingSnapshotService.upsertSnapshot(keyword, count);

        // Then
        // trim된 키워드로 조회됐는지 확인
        verify(searchRepository).findByKeyword(eq(trimmedKeyword));
        verify(searchRepository).save(any(Search.class));
    }
}