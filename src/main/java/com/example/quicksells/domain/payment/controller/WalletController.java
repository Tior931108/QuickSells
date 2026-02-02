package com.example.quicksells.domain.payment.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.payment.model.request.WithdrawRequest;
import com.example.quicksells.domain.payment.model.response.WithdrawResponse;
import com.example.quicksells.domain.payment.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WalletController {

    private final WithdrawalService withdrawalService;

    /**
     * 출금 API
     * - 내부 포인트 차감 + 거래내역(PointTransaction) 기록
     */
    @PostMapping("/wallets/withdrawals")
    public ResponseEntity<CommonResponse> withdraw(@Valid @RequestBody WithdrawRequest request, @AuthenticationPrincipal AuthUser authUser) {

        WithdrawResponse response = withdrawalService.withdraw(authUser, request.getAmount());

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("출금이 완료되었습니다.", response));
    }
}
