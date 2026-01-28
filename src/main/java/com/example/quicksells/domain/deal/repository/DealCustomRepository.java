package com.example.quicksells.domain.deal.repository;

import com.example.quicksells.domain.deal.model.response.DealCompletedResponse;
import com.example.quicksells.domain.deal.model.response.DealGetAllQueryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface DealCustomRepository {

    Page<DealGetAllQueryResponse> findPurchaseDeals(Long buyerId, Pageable pageable);

    Page<DealGetAllQueryResponse> findSaleDeals(Long sellerId, Pageable pageable);

    Page<DealGetAllQueryResponse> findAllDeals(Pageable pageable);

    List<DealCompletedResponse> findCompletedDeals(int limit);
}