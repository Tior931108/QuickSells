package com.example.quicksells.domain.search.repository;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.domain.search.model.response.SearchGetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchCustomRepository {
    //검색전용
    Page<SearchGetResponse> searchItems(String keyword, List<AppraiseStatus> appraiseStatuses, List<AuctionStatusType> auctionStatus,Long viewerId, boolean isAdmin, Pageable pageable);

}
