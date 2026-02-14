package com.example.quicksells.domain.deal.repository;

import com.example.quicksells.common.enums.StatusType;
import com.example.quicksells.domain.appraise.entity.QAppraise;
import com.example.quicksells.domain.auction.entity.QAuction;
import com.example.quicksells.domain.deal.model.response.DealCompletedResponse;
import com.example.quicksells.domain.deal.model.response.DealGetAllQueryResponse;
import com.example.quicksells.domain.item.entity.QItem;
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
        // 구매내역은 buyerId 조건만 걸면 되므로 기존대로 OK
        BooleanExpression cond =
                deal.auction.isNotNull()
                        .and(deal.auction.buyer.isNotNull())
                        .and(deal.auction.buyer.id.eq(buyerId));

        return fetchDeals(cond, pageable);
    }

    @Override
    public Page<DealGetAllQueryResponse> findSaleDeals(Long sellerId, Pageable pageable) {
        // ✅ 여기서 절대 deal.appraise.item.seller 같은 깊은 경로 쓰지 말 것!
        return fetchDealsForSale(sellerId, pageable);
    }

    @Override
    public Page<DealGetAllQueryResponse> findAllDeals(Pageable pageable) {
        return fetchDeals(null, pageable);
    }

    /**
     * ✅ 공통 fetch (조건은 외부에서 받음)
     */
    private Page<DealGetAllQueryResponse> fetchDeals(BooleanExpression condition, Pageable pageable) {

        QAuction auction = new QAuction("auction");

        QAppraise appraise = new QAppraise("appraise");
        QAppraise auctionAppraise = new QAppraise("auctionAppraise");

        QItem item = new QItem("item");
        QItem auctionItem = new QItem("auctionItem");

        // ✅ seller alias 2개 (충돌 방지 + 판매내역 조건도 여기 alias로 걸거임)
        QUser sellerA = new QUser("sellerA");
        QUser sellerB = new QUser("sellerB");
        QUser buyer = new QUser("buyer");

        // ✅ 즉시판매/경매 중 있는 값 통합
        var itemIdExpr = item.id.coalesce(auctionItem.id);
        var itemNameExpr = item.name.coalesce(auctionItem.name);
        var appraiseStatusExpr = appraise.appraiseStatus.coalesce(auctionAppraise.appraiseStatus);

        var sellerIdExpr = sellerA.id.coalesce(sellerB.id);
        var sellerNameExpr = sellerA.name.coalesce(sellerB.name);

        List<DealGetAllQueryResponse> content = queryFactory
                .select(Projections.constructor(
                        DealGetAllQueryResponse.class,
                        deal.id,
                        deal.dealPrice,
                        deal.status,
                        deal.createdAt,

                        appraiseStatusExpr,
                        auction.status,

                        itemIdExpr,
                        itemNameExpr,

                        sellerIdExpr,
                        sellerNameExpr,

                        buyer.id,
                        buyer.name
                ))
                .from(deal)

                // appraise 라인
                .leftJoin(deal.appraise, appraise)
                .leftJoin(appraise.item, item)
                .leftJoin(item.seller, sellerA)

                // auction 라인
                .leftJoin(deal.auction, auction)
                .leftJoin(auction.buyer, buyer)
                .leftJoin(auction.appraise, auctionAppraise)
                .leftJoin(auctionAppraise.item, auctionItem)
                .leftJoin(auctionItem.seller, sellerB)

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

    /**
     * ✅ 판매내역 전용 fetch
     * - 조건을 sellerA/sellerB alias로만 작성 (NPE 방지)
     */
    private Page<DealGetAllQueryResponse> fetchDealsForSale(Long sellerId, Pageable pageable) {

        QAuction auction = new QAuction("auction");

        QAppraise appraise = new QAppraise("appraise");
        QAppraise auctionAppraise = new QAppraise("auctionAppraise");

        QItem item = new QItem("item");
        QItem auctionItem = new QItem("auctionItem");

        QUser sellerA = new QUser("sellerA");
        QUser sellerB = new QUser("sellerB");
        QUser buyer = new QUser("buyer");

        var itemIdExpr = item.id.coalesce(auctionItem.id);
        var itemNameExpr = item.name.coalesce(auctionItem.name);
        var appraiseStatusExpr = appraise.appraiseStatus.coalesce(auctionAppraise.appraiseStatus);

        var sellerIdExpr = sellerA.id.coalesce(sellerB.id);
        var sellerNameExpr = sellerA.name.coalesce(sellerB.name);

        // ✅ 판매 조건: sellerA 또는 sellerB 중 하나가 sellerId면 된다
        BooleanExpression saleCond =
                sellerA.id.eq(sellerId).or(sellerB.id.eq(sellerId));

        List<DealGetAllQueryResponse> content = queryFactory
                .select(Projections.constructor(
                        DealGetAllQueryResponse.class,
                        deal.id,
                        deal.dealPrice,
                        deal.status,
                        deal.createdAt,

                        appraiseStatusExpr,
                        auction.status,

                        itemIdExpr,
                        itemNameExpr,

                        sellerIdExpr,
                        sellerNameExpr,

                        buyer.id,
                        buyer.name
                ))
                .from(deal)

                .leftJoin(deal.appraise, appraise)
                .leftJoin(appraise.item, item)
                .leftJoin(item.seller, sellerA)

                .leftJoin(deal.auction, auction)
                .leftJoin(auction.buyer, buyer)
                .leftJoin(auction.appraise, auctionAppraise)
                .leftJoin(auctionAppraise.item, auctionItem)
                .leftJoin(auctionItem.seller, sellerB)

                // ✅ where는 alias로만!
                .where(saleCond)
                .orderBy(deal.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(deal.count())
                .from(deal)

                .leftJoin(deal.appraise, appraise)
                .leftJoin(appraise.item, item)
                .leftJoin(item.seller, sellerA)

                .leftJoin(deal.auction, auction)
                .leftJoin(auction.appraise, auctionAppraise)
                .leftJoin(auctionAppraise.item, auctionItem)
                .leftJoin(auctionItem.seller, sellerB)

                .where(saleCond)
                .fetchOne();

        return new PageImpl<>(content, pageable, Objects.requireNonNullElse(total, 0L));
    }

    @Override
    public List<DealCompletedResponse> findCompletedDeals(int limit) {

        QAuction auction = new QAuction("auction");

        QAppraise appraise = new QAppraise("appraise");
        QAppraise auctionAppraise = new QAppraise("auctionAppraise");

        QItem item = new QItem("item");
        QItem auctionItem = new QItem("auctionItem");

        var itemIdExpr = item.id.coalesce(auctionItem.id);
        var itemNameExpr = item.name.coalesce(auctionItem.name);
        var appraiseStatusExpr = appraise.appraiseStatus.coalesce(auctionAppraise.appraiseStatus);

        return queryFactory
                .select(Projections.constructor(
                        DealCompletedResponse.class,
                        deal.id,
                        deal.dealPrice,
                        appraiseStatusExpr,
                        auction.status,
                        itemIdExpr,
                        itemNameExpr,
                        deal.createdAt
                ))
                .from(deal)
                .leftJoin(deal.appraise, appraise)
                .leftJoin(appraise.item, item)
                .leftJoin(deal.auction, auction)
                .leftJoin(auction.appraise, auctionAppraise)
                .leftJoin(auctionAppraise.item, auctionItem)
                .where(deal.status.eq(StatusType.SOLD))
                .orderBy(deal.createdAt.desc())
                .limit(limit)
                .fetch();
    }
}
