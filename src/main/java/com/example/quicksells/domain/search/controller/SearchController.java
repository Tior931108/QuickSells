package com.example.quicksells.domain.search.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.common.model.PageResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.search.model.response.SearchGetResponse;
import com.example.quicksells.domain.search.service.SearchCacheService;
import com.example.quicksells.domain.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "검색(search) 관리")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SearchCacheService searchCacheService;

    /**
     * 상품검색 API
     *
     * @param keyword  검색어
     * @param pageable 페이징
     * @return 페이징된 상품 결과 검색
     */
    @Operation(summary = "상품 검색 조회")
    @GetMapping("/item/searchs")
    public ResponseEntity<PageResponse> keywordGet(@AuthenticationPrincipal AuthUser authUser, @RequestParam String keyword, @PageableDefault(page = 0, size = 10) Pageable pageable) {

        //비지니스 로직
        Page<SearchGetResponse> responsesDto = searchService.search(authUser, keyword, pageable);

        // 미등록 상품 검색한 경우 응답 값
        if (responsesDto.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("등록된 상품이 없습니다.", responsesDto));
        }

        //응답 값
        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("검색 결과입니다.", responsesDto));
    }

    @GetMapping("/popular/searches")
    public ResponseEntity<CommonResponse> getPopularRankings() {

        // searchCacheService에서 Top10 검색 목록 가져오기
        List<String> popularKeywords = searchCacheService.getPopularKeywordsList();

        //응답 값
        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("인기 검색어 목록입니다.", popularKeywords));
    }
}
