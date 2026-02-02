package com.example.quicksells.domain.auction.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BidInfo {

    private Long auctionId;
    private String buyerName;
    private Integer bidPrice;
}
