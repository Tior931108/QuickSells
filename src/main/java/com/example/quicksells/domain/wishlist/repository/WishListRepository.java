package com.example.quicksells.domain.wishlist.repository;

import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.wishlist.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishListRepository extends JpaRepository<WishList, Long>, WishListCustomRepository {

    // 동일한 유저ID 상품ID의 존재 여부
    boolean existsByBuyerAndAuction(User user, Auction auction);
}
