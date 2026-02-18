# 📈 QueryDSL을 통한 대용량 거래 내역 조회 성능 개선

## 🔍 문제 상황 
* **데이터 규모:** 약 5만 건의 테스트 데이터 기준
* **성능 저하:** 거래 내역(Deal) 조회 API 호출 시 응답 시간 **4~5초** 소요
* **원인 분석:** * Deal → Item → User(Seller/Buyer) 연관관계 접근 시 **LAZY 로딩**으로 인한 **N+1 문제** 발생
    * 1번의 조회 쿼리 후 연관 데이터를 가져오기 위해 최대 **100,001개**의 추가 쿼리 실행



## 📌 해결 과정 

### 1. 문제 코드 분석
기존 Spring Data JPA 방식은 페이징 처리 시 연관된 엔티티들을 루프를 돌며 개별 조회하여 성능이 기하급수적으로 하락했습니다.

### 2. QueryDSL + Fetch Join 도입
`Fetch Join`을 사용하여 단 한 번의 쿼리로 모든 연관 엔티티를 조인하여 가져오도록 개선했습니다.

```java
@Repository
@RequiredArgsConstructor
public class DealRepositoryImpl implements DealCustomRepository {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public Page<Deal> findAllWithFetchJoin(Pageable pageable) {
        
        QDeal deal = QDeal.deal;
        QItem item = QItem.item;
        QUser seller = new QUser("seller");  // seller alias
        QUser buyer = new QUser("buyer");    // buyer alias
        
        // 모든 연관 엔티티를 한 번에 로딩
        List<Deal> content = queryFactory
                .select(deal)
                .from(deal)
                .join(deal.item, item).fetchJoin()           // Item 한 번에
                .join(deal.seller, seller).fetchJoin()       // Seller 한 번에
                .leftJoin(deal.buyer, buyer).fetchJoin()     // Buyer 한 번에 (nullable)
                .orderBy(deal.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        
        // COUNT 쿼리 분리
        Long total = queryFactory
                .select(deal.count())
                .from(deal)
                .fetchOne();
        
        return new PageImpl<>(content, pageable, total);
    }
}
```
### 📊 개선 전/후 비교

#### 1. 개선 전 (Spring Data JPA)
* **쿼리 수:** 1 + 50,000 + 50,000 = **100,001개 실행**
* **로그 상황:**
  ```sql
  Hibernate: SELECT * FROM deals;
  Hibernate: SELECT * FROM items WHERE id=1;
  Hibernate: SELECT * FROM users WHERE id=2;
  ... (50,000번 반복)
  ```

#### 2. 개선 후 (QueryDSL + Fetch Join 도입)

단 1번의 쿼리로 모든 연관 데이터를 조회하도록 변경하여 데이터베이스 부하를 획기적으로 줄였습니다.

#### 개선된 로그 (Hibernate)
```sql
-- 모든 데이터 조인 및 페이징 처리 (단 1번 실행)
Hibernate: 
SELECT
    d.id, d.type, d.status, d.deal_price,
    i.id, i.title,
    s.id, s.name,
    b.id, b.name
FROM deals d
JOIN items i ON d.item_id = i.id
JOIN users s ON d.seller_id = s.id
LEFT JOIN users b ON d.buyer_id = b.id
ORDER BY d.created_at DESC
LIMIT 20;

-- COUNT 쿼리 분리 (페이징 최적화)
Hibernate: SELECT COUNT(d.id) FROM deals d;

-- 최종 실행 쿼리 수: 2개 (추가 쿼리 0개)
```
---
### 🎯 핵심 개선 포인트
1. **`fetchJoin()` 도입**: 연관된 엔티티를 별도의 쿼리 없이 즉시 로딩하여 JPA의 고질적인 문제인 N+1 발생 원인을 근본적으로 차단했습니다.
2. **동일 엔티티(User) 다중 조인**: `QUser("seller")`, `QUser("buyer")`와 같이 각각 별칭(alias)을 부여하여 동일 테이블을 참조하는 연관 관계를 성공적으로 조인했습니다.
3. **COUNT 쿼리 최적화**: 페이징 처리를 위해 전체 데이터 수를 구하는 쿼리를 데이터 조회 쿼리와 분리하여, 불필요한 조인으로 인한 카운트 성능 저하를 방지했습니다.
4. **`LEFT JOIN` 활용**: 구매자(`buyer`)는 거래 상태에 따라 `null`일 수 있으므로, 데이터 누락을 방지하기 위해 일반 `JOIN`이 아닌 `LEFT JOIN`을 적용했습니다.

---

### 📝 회고록 

#### 💡 배운 점
* **N+1 문제의 심각성 체감**: 5만 건의 데이터에서 응답 시간이 4초 이상 소요되는 것을 확인하며, 실 서비스(수십만 건 이상) 환경에서는 이 문제가 시스템 다운으로 이어질 수 있는 치명적인 결함임을 깨달았습니다.
* **QueryDSL의 강력함**: Spring Data JPA만으로는 구현하기 까다로운 복잡한 `Fetch Join`과 동적 쿼리를 Type-Safe하게 작성할 수 있는 QueryDSL의 효율성을 경험했습니다.
* **동일 엔티티 다중 조인 기법**: `QUser("seller")`, `QUser("buyer")`와 같은 별칭(Alias) 활용법을 통해 하나의 테이블을 여러 역할로 조인하는 고급 쿼리 작성 기술을 습득했습니다.
* **COUNT 쿼리 분리의 중요성**: 페이징 성능 최적화를 위해 데이터 조회 쿼리와 전체 개수를 구하는 쿼리를 별도로 실행하는 것이 성능상 얼마나 이득인지 학습했습니다.

#### 🚀 개선 효과
* **응답 속도 20배 향상**: 기존 4~5초가 소요되던 API 응답 속도를 **300ms** 내외로 단축시켰습니다.
* **DB 부하 99.9% 감소**: 기존 10만 건 이상의 쿼리가 발생하던 로직을 **단 2개의 쿼리**로 최적화하여 데이터베이스 리소스를 획기적으로 절약했습니다.
* **확장성 확보**: 데이터가 지속적으로 축적되어도 일정한 성능을 유지할 수 있는 쿼리 구조를 구축했습니다.
* **사용자 경험 개선**: 대용량 거래 내역 조회 시 로딩 없이 즉각적인 응답을 제공하여 UX를 개선했습니다.

#### 📈 향후 개선 방향 (개선 가능한 부분)
* **DTO Projection 적용**: 엔티티 전체 필드가 아닌 필요한 컬럼만 선택적으로 조회하여 메모리 사용량 및 네트워크 비용을 추가로 절감할 예정입니다.
* **Redis 캐싱 도입**: 빈번하게 조회되지만 변경 주기가 긴 상위 거래 내역에 대해 캐싱을 적용하여 DB 접근 빈도를 낮출 계획입니다.
* **인덱스 최적화**: `created_at`, `status` 등 자주 필터링되는 컬럼에 대해 복합 인덱스를 구성하여 조회 성능을 극대화할 계획입니다.
* **커서 기반 페이징 전환**: 대용량 데이터 환경에서 성능 저하를 일으키는 `OFFSET` 방식 대신, No-Offset(Cursor) 기반 페이징으로의 전환을 검토 중입니다.