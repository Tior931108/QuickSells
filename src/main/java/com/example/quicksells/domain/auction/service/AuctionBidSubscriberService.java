package com.example.quicksells.domain.auction.service;

import com.example.quicksells.domain.auction.model.dto.BidInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RPatternTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionBidSubscriberService {

    private final RedissonClient redisson;
    private final SimpMessagingTemplate messagingTemplate;
    private static final String TOPIC_NAME_PATTERN = "topic:live:auction:bid:*";

    @EventListener(ApplicationReadyEvent.class)
    public void setUp() {

        // 래디슨에 저장된 토픽을 패턴으로 찾음
        RPatternTopic topic = redisson.getPatternTopic(TOPIC_NAME_PATTERN, new JsonJacksonCodec());

        topic.addListener(BidInfo.class, (pattern, channel, msg) -> {

            String buyerName = msg.getBuyerName();

            Integer bidPrice = msg.getBidPrice();

            // 입찰 정보
            log.info("[SUB] 새로운 입찰 메시지 수신 ▶ Channel : {} ▶ Message : 유저 {} 님이 {}원에 입찰", channel, buyerName, bidPrice);

            // 웹소켓 목적지 주소 생성
            String destination = "/topic/live/all/auction/bid/info";

            // 스톰프로 실시간 입찰 정보 전송 (휘발성)
            messagingTemplate.convertAndSend(destination, msg);
        });
    }
}
