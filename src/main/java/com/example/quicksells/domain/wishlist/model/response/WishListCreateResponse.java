package com.example.quicksells.domain.wishlist.model.response;

import com.example.quicksells.domain.wishlist.entity.WishList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class WishListCreateResponse {

    private final Long id;
    private final Long buyerId;
    private final Long auctionId;
    private final LocalDateTime createdAt;

    public static WishListCreateResponse from(WishList wishList) {
        return new WishListCreateResponse(
                wishList.getId(),
                wishList.getBuyer().getId(),
                wishList.getAuction().getId(),
                wishList.getCreatedAt()
        );
    }
}
