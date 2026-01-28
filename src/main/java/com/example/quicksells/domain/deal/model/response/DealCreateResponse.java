package com.example.quicksells.domain.deal.model.response;

import com.example.quicksells.domain.deal.entity.Deal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DealCreateResponse {

    private final Long dealId;
    private final Integer dealPrice;

    public static DealCreateResponse from(Deal deal) {
        return new DealCreateResponse(
                deal.getId(),
                deal.getDealPrice()
        );
    }
}
