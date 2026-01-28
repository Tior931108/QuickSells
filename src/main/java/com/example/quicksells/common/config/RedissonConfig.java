package com.example.quicksells.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.client.codec.StringCodec;

@Configuration
public class RedissonConfig {

    // 래디슨 클라이언트
    @Bean
    public RedissonClient redissonClient() {

        Config config = new Config();
        config.setCodec(new StringCodec()); // 문자열로 변환 (default = Kryo5Codec)
        config.setNettyThreads(Runtime.getRuntime().availableProcessors() * 2); // 서버가 실행되는 컴퓨터의 논리 코어 * 2
        config.setLockWatchdogTimeout(1500); // ttl 시간과 ttl시간/3 만큼 자동연장 (default = 30초)
        config.useSingleServer().setAddress("valkey://127.0.0.1:6379");

        return Redisson.create(config); // redisson 설정
    }
}
