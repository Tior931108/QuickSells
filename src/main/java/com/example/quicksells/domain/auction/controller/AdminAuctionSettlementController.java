package com.example.quicksells.domain.auction.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.domain.auction.model.response.AdminAuctionSettlementRetryResponse;
import com.example.quicksells.domain.auction.service.AdminAuctionSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 전용: 경매 낙찰 정산 재시도 API
 *
 * 정책:
 * - 낙찰(SUCCESSFUL_BID)은 유지
 * - 포인트 부족 등으로 정산 실패한 경우
 *   운영자가 추가 충전 유도 후 이 API로 재정산을 시도한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAuctionSettlementController {

    private final AdminAuctionSettlementService adminAuctionSettlementService;

    @PostMapping("/auctions/{auctionId}/settlements/retry")
    public ResponseEntity<CommonResponse> retrySettlement(@PathVariable Long auctionId) {

        AdminAuctionSettlementRetryResponse response = adminAuctionSettlementService.retrySettlement(auctionId);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("낙찰 정산 재시도를 성공했습니다.", response));
    }
}
