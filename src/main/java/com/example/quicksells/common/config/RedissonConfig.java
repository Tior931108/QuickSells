package com.example.quicksells.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value; // 추가됨
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.client.codec.StringCodec;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setCodec(new StringCodec());

        // 로그를 찍어 실제 주소가 뭔지 확인 (선택 사항)
        System.out.println("🚩 Connecting to Redis at: " + redisHost);

        // "redis://" 프로토콜이 중복되지 않도록 처리
        String address = redisHost.startsWith("redis://") ? redisHost : "redis://" + redisHost;
        if (!address.contains(":")) {
            address += ":6379";
        }

        config.useSingleServer().setAddress(address);
        return Redisson.create(config);
    }
}