package com.example.quicksells.domain.payment.toss;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class TossPaymentsClient {

    /**
     * 토스 전용 RestClient
     * - RestClientConfig에서 baseUrl을 고정해둠
     */
    private final RestClient restClient;

    /**
     * 토스 secretKey
     */
    @Value("${toss.payments.secret-key}")
    private String secretKey;

    /**
     * 결제 승인(confirm)
     *
     * - POST /v1/payments/confirm
     * - request: paymentKey, orderId, amount
     * - response: paymentKey, status, totalAmount 등
     */
    public TossConfirmResponse confirm(TossConfirmRequest request) {
        return restClient.post()
                .uri("/v1/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(secretKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TossConfirmResponse.class);
    }

    /**
     * 결제 취소(cancel)
     *
     * - 내부 DB 반영 실패 시 "외부 결제를 되돌리기" 위해 호출
     * - POST /v1/payments/{paymentKey}/cancel
     */
    public TossCancelResponse cancel(String paymentKey, String reason) {
        return restClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(secretKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TossCancelRequest(reason))
                .retrieve()
                .body(TossCancelResponse.class);
    }

    /**
     * Basic 인증 헤더 생성
     *
     *   중요
     * - 토스는 "secretKey:" (콜론 포함) 문자열을 base64로 인코딩하여 Basic auth로 사용
     * - 콜론 누락하면 인증 실패함
     */
    private String basicAuthHeader(String secretKey) {
        String raw = secretKey + ":";
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}