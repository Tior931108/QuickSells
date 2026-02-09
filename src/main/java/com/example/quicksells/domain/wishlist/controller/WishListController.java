package com.example.quicksells.domain.wishlist.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.common.model.SliceResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.wishlist.model.request.OneWishListDeleteRequest;
import com.example.quicksells.domain.wishlist.model.request.WishListCreateRequest;
import com.example.quicksells.domain.wishlist.model.response.MyWishListGetAllResponse;
import com.example.quicksells.domain.wishlist.model.response.WishListCreateResponse;
import com.example.quicksells.domain.wishlist.service.WishListService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관심 목록(wish_list) 관리")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WishListController {

    private final WishListService wishListService;

    @Operation(summary = "관심 목록 등록")
    @PostMapping("/wishList")
    public ResponseEntity<CommonResponse> createWishList(@RequestBody WishListCreateRequest request) {

        WishListCreateResponse response = wishListService.saveWishList(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("관심 목록 등록에 성공했습니다.", response));
    }

    @Operation(summary = "내 관심 목록 조회")
    @GetMapping("/wishList")
    public ResponseEntity<SliceResponse> getAllMyWishList(@AuthenticationPrincipal AuthUser authUser, @RequestParam Long buyerId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10")int Size) {

        Pageable pageable = PageRequest.of(page, Size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Slice<MyWishListGetAllResponse> response = wishListService.getAllMyWishList(authUser, buyerId, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(SliceResponse.success("내 관심 목록 조회에 성공했습니다.", response));
    }

    @Operation(summary = "내 관심 목록 삭제")
    @DeleteMapping("/wishList")
    public ResponseEntity<CommonResponse> deleteMyWishList(@AuthenticationPrincipal AuthUser authUser, @Valid OneWishListDeleteRequest request, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        wishListService.deleteMyWishList(authUser, request, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success(request.getIndex() + "번째 관심 목록 삭제에 성공했습니다"));
    }
}
