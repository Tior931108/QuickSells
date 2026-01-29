package com.example.quicksells.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * 토스 API 호출 전용 RestClient
     *
     *   분리 이유
     * - baseUrl을 고정
     * - (추후) timeout, logging, interceptor 등을 토스용으로만 세팅 가능
     */
    @Bean
    public RestClient tossRestClient(@Value("${toss.payments.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}