package com.example.quicksells.domain.ask.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.common.model.PageResponse;
import com.example.quicksells.domain.ask.model.request.AskCreateRequest;
import com.example.quicksells.domain.ask.model.request.AskUpdateRequest;
import com.example.quicksells.domain.ask.model.response.*;
import com.example.quicksells.domain.ask.service.AskService;
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

@Tag(name = "문의(ask) 관리")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AskController {

    private final AskService askService;

    /**
     * 문의 생성
     */
    @Operation(summary = "문의 생성")
    @PostMapping("/asks")
    public ResponseEntity<CommonResponse> createAsk(@Valid @RequestBody AskCreateRequest request, @AuthenticationPrincipal AuthUser authUser) {

        AskCreateResponse response = askService.createAsk(request, authUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("문의 생성에 성공했습니다.", response));
    }

    /**
     * 문의 전체 조회 (페이징)
     */
    @Operation(summary = "문의 전체 조회")
    @GetMapping("/asks")
    public ResponseEntity<PageResponse> getAllAsks(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "desc") String order) {

        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        Page<AskGetAllResponse> response = askService.getAllAsks(pageable);

        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("문의 전체 조회에 성공했습니다.", response));
    }

    /**
     * 문의 상세 조회 (본인만 가능)
     */
    @Operation(summary = "문의 상세 조회")
    @GetMapping("/asks/{id}")
    public ResponseEntity<CommonResponse> getAsk(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {

        AskGetResponse response = askService.getAsk(id, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("문의 상세 조회에 성공했습니다.", response));
    }

    /**
     * 사용자 문의 상세 조회 (관리자)
     */
    @Operation(summary = "사용자 문의 상세 조회")
    @GetMapping("/admin/asks/{id}")
    public ResponseEntity<CommonResponse> getUserAsk(@PathVariable Long id) {

        AskAdminGetResponse response = askService.getUserAsk(id);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("사용자 문의 상세 조회에 성공했습니다.", response));
    }

    /**
     * 문의 수정 (본인만 가능)
     */
    @Operation(summary = "문의 수정")
    @PatchMapping("/asks/{id}")
    public ResponseEntity<CommonResponse> updateAsk(@PathVariable Long id, @Valid @RequestBody AskUpdateRequest request, @AuthenticationPrincipal AuthUser authUser) {

        AskUpdateReponse response = askService.updateAsk(id, request, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("문의 수정에 성공했습니다.", response));
    }

    /**
     * 문의 삭제 (본인만 가능)
     */
    @Operation(summary = "문의 삭제")
    @DeleteMapping("/asks/{id}")
    public ResponseEntity<CommonResponse> deleteAsk(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {

        askService.deleteAsk(id, authUser);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("문의 삭제에 성공했습니다."));
    }
}
