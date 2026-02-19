package com.example.quicksells.domain.auction.service;

import com.example.quicksells.domain.auction.model.dto.BidInfo;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuctionBidPublisher {

    private final RedissonClient redisson;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBidInfo(BidInfo bidInfo) {

        String topicName = "topic:auction:bid:" + bidInfo.getAuctionId();

        RTopic topic = redisson.getTopic(topicName, new JsonJacksonCodec());

        topic.publish(bidInfo);
    }
}
