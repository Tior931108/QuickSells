package com.example.quicksells.domain.payment.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.common.model.PageResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.payment.model.request.AdminPointGrantRequest;
import com.example.quicksells.domain.payment.model.request.WithdrawRequest;
import com.example.quicksells.domain.payment.model.response.AdminPointGrantResponse;
import com.example.quicksells.domain.payment.model.response.PointTransactionGetResponse;
import com.example.quicksells.domain.payment.model.response.WalletGetResponse;
import com.example.quicksells.domain.payment.model.response.WithdrawResponse;
import com.example.quicksells.domain.payment.service.PointWalletService;
import com.example.quicksells.domain.payment.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 포인트 지갑(Wallet) 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WalletController {

    private final WithdrawalService withdrawalService;
    private final PointWalletService pointWalletService;

    /**
     * 출금 API
     * - 내부 포인트 차감 + 거래내역(PointTransaction) 기록
     */
    @Operation(summary = "포인트 출금 : 내부 포인트 차감 + 거래내역(PointTransaction) 기록")
    @PostMapping("/wallets/withdrawals")
    public ResponseEntity<CommonResponse> withdraw(@Valid @RequestBody WithdrawRequest request, @AuthenticationPrincipal AuthUser authUser) {

        WithdrawResponse response = withdrawalService.withdraw(authUser, request.getAmount());

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("출금이 완료되었습니다.", response));
    }

    /**
     * 내 지갑 조회
     */
    @Operation(summary = "내 지갑 조회 ")
    @GetMapping("/wallets/me")
    public ResponseEntity<CommonResponse> getMyWallet(@AuthenticationPrincipal AuthUser authUser) {

        WalletGetResponse response = pointWalletService.getMyWalletResponse(authUser.getId());

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("내 지갑 조회를 성공하였습니다.", response));
    }

    /**
     * 내 포인트 거래내역 조회 (페이징)
     */
    @Operation(summary = "내 포인트 거래내역 조회")
    @GetMapping("/wallets/transactions")
    public ResponseEntity<PageResponse> getMyTransactions(@AuthenticationPrincipal AuthUser authUser, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        Page<PointTransactionGetResponse> result = pointWalletService.getMyTransactionsResponse(authUser.getId(), page, size);

        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("내 포인트 거래내역 조회를 성공하였습니다.", result));
    }

    /**
     * 관리자: 포인트 지급
     */
    @Operation(summary = "관리자 : 포인트 지급")
    @PostMapping("/admin/wallets/{userId}/grant")
    public ResponseEntity<CommonResponse> grantPoint(@PathVariable Long userId, @Valid @RequestBody AdminPointGrantRequest request) {

        AdminPointGrantResponse response = pointWalletService.grantPointResponse(userId, request.getAmount());

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("관리자 포인트 지급에 성공하였습니다.", response));
    }

    /**
     * 관리자: 특정 유저 지갑 조회
     */
    @Operation(summary = "관리자 : 특정 유저 지갑 조회")
    @GetMapping("/admin/wallets/{userId}")
    public ResponseEntity<CommonResponse> getUserWallet(@PathVariable Long userId) {

        WalletGetResponse response = pointWalletService.getUserWalletResponse(userId);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("유저 지갑 조회를 성공하였습니다.", response));
    }

    /**
     * 관리자: 특정 유저 거래내역 조회 (페이징)
     */
    @Operation(summary = "관리자 : 특정 유저 거래내역 조회")
    @GetMapping("/admin/wallets/{userId}/transactions")
    public ResponseEntity<PageResponse> getUserTransactions(@PathVariable Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        Page<PointTransactionGetResponse> result = pointWalletService.getUserTransactionsResponse(userId, page, size);

        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.success("유저 거래내역 조회를 성공하였습니다.", result));
    }
}
