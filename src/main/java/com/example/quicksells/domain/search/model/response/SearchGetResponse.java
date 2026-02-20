package com.example.quicksells.domain.search.model.response;

import com.example.quicksells.domain.item.entity.Item;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SearchGetResponse {
    private Long id;
    private String name;
    private String appraiseStatus;
    private String auctionStatus;


    public SearchGetResponse(Long id, String name,String appraiseStatus, String auctionStatus) {
        this.id = id;
        this.name = name;
        this.appraiseStatus = appraiseStatus;
        this.auctionStatus = auctionStatus;
    }

    public static SearchGetResponse from(Item item) {
        return new SearchGetResponse(
                item.getId(),
                item.getName(),
                null,
                null
        );
    }
}