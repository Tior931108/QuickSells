package com.example.quicksells.domain.auction.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.common.model.PageResponse;
import com.example.quicksells.common.model.SliceResponse;
import com.example.quicksells.domain.auction.model.request.AuctionCreateRequest;
import com.example.quicksells.domain.auction.model.request.AuctionSearchFilterRequest;
import com.example.quicksells.domain.auction.model.request.AuctionUpdateRequest;
import com.example.quicksells.domain.auction.model.response.*;
import com.example.quicksells.domain.auction.service.AuctionService;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "경매(Auction) 관리")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @Operation(summary = "경매 생성")
    @PostMapping("/auctions")
    public ResponseEntity<CommonResponse> createAuction(@Valid @RequestBody AuctionCreateRequest request) {

        AuctionCreateResponse response = auctionService.saveAuction(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("경매장 등록에 성공했습니다.", response));
    }

    @Operation(summary = "경매 내역 전체 조회")
    @GetMapping("/auctions")
    public ResponseEntity<PageResponse> getAllAuction(@Valid AuctionSearchFilterRequest request, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");

        Page<AuctionGetAllResponse> response = auctionService.getAllAuction(pageable, request);

        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("경매 목록 조회에 성공했습니다.", response));
    }

    @Operation(summary = "경매 내역 상세 조회")
    @GetMapping("/auctions/{id}")
    public ResponseEntity<CommonResponse> getAuction(@PathVariable Long id) {

        AuctionGetResponse response = auctionService.getAuction(id);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("경매 상세 조회에 성공했습니다.", response));
    }

    @Operation(summary = "내 경매 입찰 내역 조회")
    @GetMapping("/auctionHistory")
    public ResponseEntity<SliceResponse> getAllAuctionHistory(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @AuthenticationPrincipal AuthUser authUser, @RequestParam Long buyerId) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "updatedAt");

        Slice<AuctionHistoryGetAllResponse> response = auctionService.GetAllAuctionHistory(pageable, authUser, buyerId);

        return ResponseEntity.status(HttpStatus.OK).body(SliceResponse.success("내 경매 입찰 내역 조회에 성공했습니다", response));
    }

    @Operation(summary = "경매 입찰(구매자)")
    @PostMapping("/auctions/{id}/bid")
    public ResponseEntity<CommonResponse> updateBidPrice(@PathVariable Long id, @Valid @RequestBody AuctionUpdateRequest request, @AuthenticationPrincipal AuthUser authUser) {

        AuctionUpdateResponse response = auctionService.updateBidPrice(id, request, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("상품 입찰에 성공했습니다.", response));
    }

    @Operation(summary = "경매 삭제(관리자)")
    @DeleteMapping("/admin/auctions/{id}")
    public ResponseEntity<CommonResponse> deleteAuction(@PathVariable Long id) {

        auctionService.deleteAuction(id);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("경매 삭제에 성공했습니다."));
    }

}
