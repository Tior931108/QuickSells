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
        config.setNettyThreads(Runtime.getRuntime().availableProcessors() * 2);
        config.setLockWatchdogTimeout(1500);
        config.useSingleServer().setAddress("redis://" + redisHost + ":6379");

        return Redisson.create(config);
    }
}