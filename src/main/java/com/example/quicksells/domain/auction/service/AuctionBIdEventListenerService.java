package com.example.quicksells.domain.auction.service;

import com.example.quicksells.domain.auction.model.dto.BidInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionBIdEventListenerService {

    private final RedissonClient redisson;
    private static final String TOPIC_NAME = "topic:auction:bid:";

    // 디폴트지만 명시적으로 커밋 이후 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(BidInfo bidInfo) {

        Long auctionId = bidInfo.getAuctionId();
        String channelName = TOPIC_NAME + auctionId;

        // 토픽, 제이슨 직렬화
        RTopic topic = redisson.getTopic(channelName,  new JsonJacksonCodec());

        // 입찰이벤트 토픽 발행
        Long receiverCount = topic.publish(bidInfo);

        log.info("[PUB] 이벤트 발행 성공 ▶ Target Channel : {}, ▶ 수신자 수 : {}명", channelName,  receiverCount);
    }
}
