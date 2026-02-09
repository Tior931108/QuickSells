package com.example.quicksells.domain.item.model.response;

import com.example.quicksells.domain.item.model.dto.ItemInfoDto;
import com.example.quicksells.domain.item.entity.Item;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ItemGetDetailResponse {
    private final Long id;
    private final Long userId;
    private final ItemInfoDto user;

    public static ItemGetDetailResponse from(Item item) {
        return new ItemGetDetailResponse(
                item.getId(),
                item.getSeller().getId(),
                new ItemInfoDto(
                        item.getName(),
                        item.getHopePrice(),
                        item.getDescription(),
                        item.getImage(),
                        item.getSeller().getRole(),
                        item.getCreatedAt()));
    }
}
