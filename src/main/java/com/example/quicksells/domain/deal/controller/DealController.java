package com.example.quicksells.domain.deal.controller;

import com.example.quicksells.common.enums.DealType;
import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.common.model.PageResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.deal.model.request.DealCreateRequest;
import com.example.quicksells.domain.deal.model.response.DealCompletedResponse;
import com.example.quicksells.domain.deal.model.response.DealCreateResponse;
import com.example.quicksells.domain.deal.model.response.DealGetAllQueryResponse;
import com.example.quicksells.domain.deal.model.response.DealGetResponse;
import com.example.quicksells.domain.deal.service.DealService;
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

@Tag(name = "거래(deal) 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DealController {

    private final DealService dealService;

    /**
     * 거래 생성 API
     */
    @Operation(summary = "거래 생성")
    @PostMapping("/deals")
    public ResponseEntity<CommonResponse> createDeal(@RequestBody DealCreateRequest request) {

        DealCreateResponse response = dealService.createDeal(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("거래가 생성되었습니다.", response));
    }

    /**
     * 거래 내역 상세 조회
     */
    @Operation(summary = "거래 내역 상세 조회")
    @GetMapping("/deals/{id}")
    public ResponseEntity<CommonResponse> getDealDetail(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {

        DealGetResponse response = dealService.getDealDetail(id, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("거래 상세 조회를 성공했습니다", response));
    }

    /**
     * 거래 조회 (구매 / 판매)
     */
    @Operation(summary = "거래 내역 (구매/판매) 조회")
    @GetMapping("/deals")
    public ResponseEntity<PageResponse> getDeals(@RequestParam DealType type, @AuthenticationPrincipal AuthUser user, @PageableDefault(size = 10) Pageable pageable) {

        Page<DealGetAllQueryResponse> response = dealService.getDeals(type, user, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("거래 목록 조회를 성공하였습니다.", response));
    }

    /**
     * 완료된 거래 조회(판매)
     */
    @Operation(summary = "거래 판매완료 조회")
    @GetMapping("/deals/completed")
    public ResponseEntity<CommonResponse> getCompletedDeals(@RequestParam(defaultValue = "10") int limit) {

        List<DealCompletedResponse> response = dealService.getCompletedDeals(limit);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("판매 완료된 거래 목록 조회를 성공하였습니다.", response));
    }
}
