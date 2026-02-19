package com.example.quicksells.domain.auction.service;

import com.example.quicksells.domain.auction.model.dto.BidInfo;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionBidPublisher {

    private final RedissonClient redisson;

    @EventListener
    public void handleBidInfo(BidInfo bidInfo) {

        String topicName = "topic:auction:bid:" + bidInfo.getAuctionId();

        RTopic topic = redisson.getTopic(topicName, new JsonJacksonCodec());

        topic.publish(bidInfo);
    }
}
