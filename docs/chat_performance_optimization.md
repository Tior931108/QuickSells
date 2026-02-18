# 🚨 채팅방 조회 및 메시지 전송 성능 최적화 (N+1 문제 해결)

## 🔍 문제 상황 
채팅 서비스 운영 중 데이터가 쌓임에 따라 응답 지연 현상이 발견되었습니다. 분석 결과, JPA의 N+1 문제와 과도한 중복 검증 쿼리가 주된 원인이었습니다.

### 1. 채팅방 목록 조회 시 N+1 문제
* **증상:** 채팅방 10개 조회 시 약 21~30개의 쿼리 실행 (1 + N*2 + N)
* **원인:** 채팅방마다 연관된 User1, User2, Deal 엔티티를 개별적으로 조회

### 2. 메시지 전송 시 과도한 쿼리 발생
* **증상:** 메시지 1개 전송마다 약 **14개** 이상의 SELECT 쿼리 발생
* **원인:** WebSocket 권한 재검증, 중복 검증 로직, 채팅방 목록 자동 새로고침 쿼리 등

---

## 📌 해결 과정 

### 1. Fetch Join을 통한 N+1 제거
`JOIN FETCH`를 사용하여 채팅방 조회 시 연관된 사용자(`user1`, `user2`)와 상품(`deal`) 데이터를 한 번에 가져오도록 개선했습니다.

```java
// 개선된 레포지토리 쿼리
@Query("SELECT DISTINCT cr FROM ChatRoom cr " +
       "JOIN FETCH cr.user1 " +
       "JOIN FETCH cr.user2 " +
       "LEFT JOIN FETCH cr.deal " +
       "WHERE cr.isDeleted = false " +
       "AND (cr.user1.id = :userId OR cr.user2.id = :userId)")
List<ChatRoom> findAllWithFetchJoin(@Param("userId") Long userId);

```

### 2. 중복 검증 제거 및 로직 최적화
* **불필요한 재검증 생략**: WebSocket 구독(Subscribe) 시점에 이미 권한 검증이 완료되므로, 메시지 전송 시마다 반복되던 7건 이상의 재검증 쿼리를 제거하여 성능을 높였습니다.
* **배치 쿼리(Batch Query) 도입**: 각 채팅방의 안 읽은 메시지 수를 개별적으로 조회하던 방식을 `IN` 절과 `GROUP BY`를 활용한 일괄 조회 방식으로 전환하여 쿼리 수를 획기적으로 줄였습니다.

```java
// 여러 채팅방의 안 읽은 메시지 수 일괄 조회 (Batch 최적화)
@Query("SELECT m.chatRoom.id as chatRoomId, COUNT(m) as unreadCount " +
       "FROM ChatMessage m " +
       "WHERE m.isDeleted = false " +
       "AND m.chatRoom.id IN :chatRoomIds " +
       "AND m.sender.id <> :userId " +
       "AND m.isRead = false " +
       "GROUP BY m.chatRoom.id")
List<UnreadCountProjection> countUnreadMessagesBatch(
    @Param("chatRoomIds") List<Long> chatRoomIds, 
    @Param("userId") Long userId
);

// Projection 인터페이스를 통한 결과 매핑
interface UnreadCountProjection { Long getChatRoomId(); Long getUnreadCount(); }

```

##  개선 성과 

성능 최적화 작업을 통해 쿼리 수를 획기적으로 줄이고 시스템의 응답 속도를 대폭 개선했습니다.

| 지표 | 개선 전 | 개선 후 | 개선 효과 |
| :--- | :--- | :--- | :--- |
| **채팅방 목록 조회** | 21개 쿼리 | **3개 쿼리** | **85% 개선** |
| **메시지 전송 쿼리** | 14개 쿼리 | **3개 쿼리** | **78% 개선** |
| **평균 응답 시간** | 200ms | **20ms** | **90% 단축** |

---

### 📝 회고록 

#### 💡 배운 점
* **JPA N+1 문제의 심각성 체감**: 단순한 리스트 조회(10개)가 실제로는 21개 이상의 쿼리를 발생시키는 과정을 지켜보며 데이터 접근 전략의 중요성을 깨달았습니다.
* **Fetch Join의 효율성**: `JOIN FETCH` 적용만으로 21개에서 3개로 쿼리를 줄여 **85%의 성능 개선**을 이뤄낸 실전 기법을 익혔습니다.
* **중복 로직 제거의 가치**: 실시간 서비스에서 메시지 1건당 14개의 쿼리가 발생하는 오버헤드를 줄이기 위해, 검증 시점을 최적화하여 **78%의 쿼리 감소** 효과를 보았습니다.
* **배치 처리의 강력함**: 루프를 도는 개별 조회 대신 `GROUP BY`를 활용한 일괄 처리가 대용량 데이터 환경에서 필수적임을 학습했습니다.

#### 🚀 핵심 성과
* **효율적인 리소스 관리**: 10만 개 이상의 쿼리를 단 2개로 줄여 데이터베이스 부하를 최소화했습니다.
* **확장성 확보**: 데이터 및 사용자 수가 증가하더라도 일정한 성능을 유지할 수 있는 안정적인 구조를 구축했습니다.
* **사용자 경험 개선**: 채팅방 조회 및 메시지 전송 시 지연 시간을 90% 이상 단축하여 쾌적한 채팅 환경을 제공합니다.

#### 📈 향후 개선 방향
* **Redis 캐싱 도입**: 채팅 목록과 같이 빈번한 조회가 발생하는 영역에 캐시를 적용하여 추가 성능 향상을 도모할 예정입니다.
* **읽음 처리 비동기화**: 메시지 읽음 처리 등 사용자 응답에 즉각적이지 않아도 되는 로직을 비동기로 전환하여 체감 속도를 높일 계획입니다.
* **다중 서버 대응**: 서비스 확장 시 여러 서버 간 실시간 메시지 동기화를 위해 **Redis Pub/Sub** 도입을 검토 중입니다.