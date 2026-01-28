package com.example.quicksells.domain.auction.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.common.model.PageResponse;
import com.example.quicksells.domain.auction.model.request.AuctionCreateRequest;
import com.example.quicksells.domain.auction.model.request.AuctionSearchFilterRequest;
import com.example.quicksells.domain.auction.model.request.AuctionUpdateRequest;
import com.example.quicksells.domain.auction.model.response.AuctionCreateResponse;
import com.example.quicksells.domain.auction.model.response.AuctionGetAllResponse;
import com.example.quicksells.domain.auction.model.response.AuctionGetResponse;
import com.example.quicksells.domain.auction.model.response.AuctionUpdateResponse;
import com.example.quicksells.domain.auction.service.AuctionService;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

        AuctionCreateResponse result = auctionService.saveAuction(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("경매장 등록에 성공했습니다.", result));
    }

    @Operation(summary = "경매 내역 전체 조회")
    @GetMapping("/auctions")
    public ResponseEntity<PageResponse> getAllAuction(@Valid @RequestBody(required = false) AuctionSearchFilterRequest request, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "0") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");

        Page<AuctionGetAllResponse> result = auctionService.getAllAuction(pageable, request);

        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("경매 목록 조회에 성공했습니다.", result));
    }

    @Operation(summary = "경매 내역 상세 조회")
    @GetMapping("/auctions/{id}")
    public ResponseEntity<CommonResponse> getAuction(@PathVariable Long id) {

        AuctionGetResponse result = auctionService.getAuction(id);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("경매 상세 조회에 성공했습니다.", result));
    }

    @Operation(summary = "경매 입찰(구매자)")
    @PostMapping("/auctions/{id}/bid")
    public ResponseEntity<CommonResponse> updateBidPrice(@PathVariable Long id, @Valid @RequestBody AuctionUpdateRequest request, @AuthenticationPrincipal AuthUser authUser) {

        AuctionUpdateResponse result = auctionService.updateBidPrice(id, request, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("상품 입찰에 성공했습니다.", result));
    }

    @Operation(summary = "경매 삭제(관리자)")
    @DeleteMapping("/admin/auctions/{id}")
    public ResponseEntity<CommonResponse> deleteAuction(@PathVariable Long id) {

        auctionService.deleteAuction(id);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("경매 삭제에 성공했습니다."));
    }

}
