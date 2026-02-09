package com.example.quicksells.domain.payment.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.payment.model.request.PaymentConfirmRequest;
import com.example.quicksells.domain.payment.model.request.PaymentOrderCreateRequest;
import com.example.quicksells.domain.payment.model.response.PaymentConfigResponse;
import com.example.quicksells.domain.payment.model.response.PaymentConfirmResponse;
import com.example.quicksells.domain.payment.model.response.PaymentOrderCreateResponse;
import com.example.quicksells.domain.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 결제(Payment) 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    /**
     * 토스 clientKey
     * - 공개키 성격이라 테스트 편의를 위해 내려줄 수 있음
     */
    @Value("${toss.payments.client-key}")
    private String tossClientKey;

    /**
     * 주문 생성(READY)
     *
     *  사용 시나리오(백엔드 기준)
     * 1) 클라이언트가 충전 금액(최소 10,000원 이상)을 서버에 요청
     * 2) 서버가 orderId 생성 + Payment(READY) 저장
     * 3) 클라이언트는 이 orderId를 사용해 결제창을 오픈한다
     */
    @Operation(summary = "토스페이 API 주문(order) 기록 생성(READY)")
    @PostMapping("/orders")
    public ResponseEntity<CommonResponse> createOrder(@AuthenticationPrincipal AuthUser authUser, @Valid @RequestBody PaymentOrderCreateRequest request) {

        PaymentOrderCreateResponse response = paymentService.createOrder(authUser.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success("주문 생성을 성공했습니다.", response));
    }

    /**
     * 결제 승인(confirm)
     *
     *  사용 시나리오
     * 1) 결제 성공 시 토스가 successUrl로 리다이렉트하면서
     *    paymentKey, orderId, amount를 쿼리 파라미터로 전달한다.
     * 2) 클라이언트가 해당 값들을 서버로 보내면,
     * 3) 서버가 토스 confirm API를 호출해 결제를 최종 승인한다.
     * 4) 승인 성공 시 포인트 충전 + 내역 기록까지 완료한다.
     *
     *  안정성 처리
     * - DB 처리 실패 시 토스 cancel로 외부 결제를 롤백 시도한다.
     */
    @Operation(summary = "토스페이 API 결제 승인(confirm)")
    @PostMapping("/confirm")
    public ResponseEntity<CommonResponse> confirm(@AuthenticationPrincipal AuthUser authUser, @Valid @RequestBody PaymentConfirmRequest request) {

        PaymentConfirmResponse response = paymentService.confirm(authUser.getId(), request);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("결제 승인을 성공했습니다.", response));
    }

    /**
     * 토스 clientKey 조회 (테스트 편의용)
     *
     *  목적
     * - 프론트 협업이 없는 상태에서 백엔드 단독으로 결제창 테스트를 쉽게 하기 위함
     * - toss-test.html이 prompt 입력 없이 서버에서 clientKey를 받아 사용한다.
     *
     *  보안
     * - clientKey만 제공 (secretKey는 절대 제공 금지)
     */
    @Operation(summary = "토스페이 API clientKey 조회")
    @GetMapping("/config")
    public ResponseEntity<CommonResponse> config() {

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("토스 설정 조회 성공!", PaymentConfigResponse.from(tossClientKey)));
    }

}