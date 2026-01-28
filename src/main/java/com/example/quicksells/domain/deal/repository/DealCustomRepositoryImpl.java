package com.example.quicksells.domain.deal.repository;

import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.domain.deal.model.response.DealCompletedResponse;
import com.example.quicksells.domain.deal.model.response.DealGetAllQueryResponse;
import com.example.quicksells.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Objects;
import static com.example.quicksells.domain.deal.entity.QDeal.deal;

@RequiredArgsConstructor
public class DealCustomRepositoryImpl implements DealCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<DealGetAllQueryResponse> findPurchaseDeals(Long buyerId, Pageable pageable) {
        // 구매 내역 = 경매 구매자 기준
        return fetchDeals(deal.auction.isNotNull()
                .and(deal.auction.buyer.isNotNull())
                .and(deal.auction.buyer.id.eq(buyerId)), pageable);
    }

    @Override
    public Page<DealGetAllQueryResponse> findSaleDeals(Long sellerId, Pageable pageable) {
        // 판매 내역 = 아이템 판매자 기준
        return fetchDeals(deal.appraise.item.seller.id.eq(sellerId), pageable);
    }

    @Override
    public Page<DealGetAllQueryResponse> findAllDeals(Pageable pageable) {
        return fetchDeals(null, pageable);
    }

    private Page<DealGetAllQueryResponse> fetchDeals(BooleanExpression condition, Pageable pageable) {

        QUser seller = new QUser("seller");
        QUser buyer = new QUser("buyer");

        List<DealGetAllQueryResponse> content = queryFactory
                .select(Projections.constructor(
                        DealGetAllQueryResponse.class,
                        // Deal
                        deal.id,
                        deal.dealPrice,
                        deal.status,
                        deal.createdAt,

                        // Derived status source
                        deal.appraise.appraiseStatus,
                        deal.auction.status,

                        // Item
                        deal.appraise.item.id,
                        deal.appraise.item.name,

                        // Seller
                        seller.id,
                        seller.name,

                        // Buyer (nullable)
                        buyer.id,
                        buyer.name
                ))
                .from(deal)
                .join(deal.appraise)              // deal -> appraise
                .join(deal.appraise.item)         // appraise -> item
                .join(deal.appraise.item.seller, seller)
                .leftJoin(deal.auction)           // deal -> auction (nullable)
                .leftJoin(deal.auction.buyer, buyer)
                .where(condition)
                .orderBy(deal.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(deal.count())
                .from(deal)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, Objects.requireNonNullElse(total, 0L));
    }

    @Override
    public List<DealCompletedResponse> findCompletedDeals(int limit) {

        // 완료 거래(SOLD)만: 즉시/경매 구분은 appraiseStatus/auctionStatus로 표시
        return queryFactory
                .select(Projections.constructor(
                        DealCompletedResponse.class,
                        deal.id,
                        deal.dealPrice,
                        deal.appraise.appraiseStatus,
                        deal.auction.status,
                        deal.appraise.item.id,
                        deal.appraise.item.name,
                        deal.createdAt
                ))
                .from(deal)
                .join(deal.appraise)
                .join(deal.appraise.item)
                .leftJoin(deal.auction)
                .where(deal.status.eq(StatusType.SOLD))
                .orderBy(deal.createdAt.desc())
                .limit(limit)
                .fetch();
    }
}