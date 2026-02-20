package com.example.quicksells.domain.item.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.common.model.PageResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.item.model.request.ItemCreatedRequest;
import com.example.quicksells.domain.item.model.request.ItemUpdateRequest;
import com.example.quicksells.domain.item.model.response.ItemCreatedResponse;
import com.example.quicksells.domain.item.model.response.ItemGetDetailResponse;
import com.example.quicksells.domain.item.model.response.ItemGetListResponse;
import com.example.quicksells.domain.item.model.response.ItemUpdateResponse;
import com.example.quicksells.domain.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "상품(item) 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")

public class ItemController {
    private final ItemService itemService;

    /**
     * 상품 등록 API
     *
     * @param authUser
     * @param request
     * @return
     */
    @Operation(summary = "상품 등록")
    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse> createItem(@AuthenticationPrincipal AuthUser authUser, @Valid @RequestPart("request") ItemCreatedRequest request, @RequestPart(value = "files") MultipartFile image) {

        //생성 비지니스 핵심 로직
        ItemCreatedResponse response = itemService.createItem(authUser, request, image);

        //201 상태 코드 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("상품 등록 성공하셨습니다.", response));
    }

    /**
     * 상품 상세 조회 API
     *
     * @param id
     * @return
     */
    @Operation(summary = "상품 상세 조회 (관리자)")
    @GetMapping("/admin/items/{id}")
    public ResponseEntity<CommonResponse> getDetailItem(@PathVariable Long id) {

        //상세 조회 비지니스 핵심 로직
        ItemGetDetailResponse response = itemService.getDetailItem(id);

        //200 상태 코드 반환
        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("상품 조회 성공하셨습니다.", response));
    }

    /**
     * 페이징 적용한 목록 조회
     *
     * @param pageable
     * @return
     */
    @Operation(summary = "상품 전체 조회 (관리자)")
    @GetMapping("/items")
    public ResponseEntity<PageResponse> getAll(@PageableDefault(page = 0, size = 10) Pageable pageable) {

        //페이징 처리된 상품 목록 조회 로직
        Page<ItemGetListResponse> itemList = itemService.getAll(pageable);

        //200 상태 코드 반환
        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("상품 목록 조회 성공하셨습니다.", itemList));
    }

    /**
     * 내 등록 상품 상세 조회 기능
     *
     * @param id       조회할 상품의 id
     * @param authUser 로그인한 사용자 정보
     * @return 나의 상품 상세 정보 담은 응답 객체
     */
    //상품 등록 상세 조회
    @Operation(summary = "나의 상품 상세 조회")
    @GetMapping("/my/items/{id}")
    public ResponseEntity<CommonResponse> getMyDetail(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {

        //나의 등록 상세 조회 비지니스 핵심 로직
        ItemGetDetailResponse response = itemService.getMyDetail(id, authUser);

        //200 상태 코드 반환
        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("나의 등록 상품 조회 성공하셨습니다.", response));
    }

    /**
     *
     * @param authUser 로그인한 사용자 정보
     * @param pageable 페이징
     * @return 페이징 적용 된 상품 목록 담은 응답 객체
     */
    //상품 등록 목록 조회
    @Operation(summary = "나의 상품 전체 조회")
    @GetMapping("/my/items")
    public ResponseEntity<PageResponse> getMyItemList(@AuthenticationPrincipal AuthUser authUser, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        // 나의 전체 목록 비지니스 로직
        Page<ItemGetListResponse> itemsList = itemService.getMyItemList(authUser, pageable);

        // 페이징 적용된 200 상태 코드 반환
        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("나의 등록 상품 조회 성공하셨습니다.", itemsList));
    }

    /**
     * 상품 수정 API
     *
     * @param authUser 사용자 정보
     * @param id       수정하려는 상품 ID
     * @return 수정된 상품 정보 담은 응답 객체
     */
    @Operation(summary = "상품 수정")
    @PatchMapping(value = "/items/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse> updateItem(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, @Valid @RequestPart(value = "request") ItemUpdateRequest request, @RequestPart(value = "image", required = false) MultipartFile image) {

        //비지니스 로직
        ItemUpdateResponse response = itemService.updateItem(authUser, id, request, image);

        //200 상태 코드 반환
        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("상품 수정 성공하셨습니다.", response));
    }

    /**
     * 상품 삭제 API
     *
     * @param id       삭제하려는 상품 ID
     * @param authUser 로그인한 사용자 정보
     * @return 삭제 성공 메세지를 담은 공통 응답 객체
     */
    @Operation(summary = "상품 삭제")
    @DeleteMapping("/items/{id}")
    public ResponseEntity<CommonResponse> deleteItem(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {

        //비지니스 로직
        itemService.deleteItem(id, authUser);

        //성공적 삭제 메세지 반환
        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("상품 삭제 성공하셨습니다."));
    }
}
