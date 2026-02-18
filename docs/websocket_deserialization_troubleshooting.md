# 🚨 WebSocket 메시지 전송 시 Jackson 역직렬화 에러 발생

### ❗ 문제 상황
**에러 메시지:**
`Could not read JSON: Cannot construct instance of com.example.quicksells.domain.auth.model.dto.AuthUser (no Creators, like default constructor, exist) at SimpleBeanPropertyDefinition.construct(...)`

* 클라이언트에서 `{"content":"안녕하세요"}` 메시지 전송
* 서버에서 메시지 수신 실패 및 WebSocket 연결 끊김
* 콘솔에 Jackson 역직렬화 에러 발생

---

### 🤔 문제 원인
**@Payload와 @AuthenticationPrincipal 동시 사용 시 역직렬화 충돌**

```java
// 문제가 발생한 코드
@MessageMapping("/chat/message")
public void sendMessage(
        @Payload ChatMessageRequest request,              // JSON 페이로드
        @AuthenticationPrincipal AuthUser authUser) {     // 인증 정보
    // ...
}
```

**원인 분석:**

1. Spring의 메시지 컨버터가 두 파라미터를 모두 메시지 페이로드에서 역직렬화 시도
2. `{"content":"안녕하세요"}` → ChatMessageRequest (정상)
3. `{"content":"안녕하세요"}` → AuthUser (실패!)
4. AuthUser는 생성자 기반 객체인데 JSON에 id, email, role 필드가 없어서 NPE 발생

**근본 원인:**

- @AuthenticationPrincipal은 SecurityContext에서 가져와야 하는데, Jackson이 JSON 역직렬화를 시도
- STOMP 메시징 환경에서 @AuthenticationPrincipal의 특수한 처리가 제대로 작동하지 않음

---

### 📌해결과정

#### 1차 시도: @Payload 명시 (실패)

```java
@MessageMapping("/chat/message")
public void sendMessage(
        @Payload ChatMessageRequest request,           // 명시적 페이로드
        @AuthenticationPrincipal AuthUser authUser) {  // 여전히 실패
    // 동일한 에러 발생
}
```

→ @Payload를 명시해도 @AuthenticationPrincipal 역직렬화 문제 해결 안됨

#### 2차 시도: Principal 사용 후 수동 추출 (성공)

```java
@MessageMapping("/chat/message")
public void sendMessage(
        @Payload ChatMessageRequest request,  // JSON에서 역직렬화
        Principal principal) {                 // SecurityContext에서 주입
    
    // AuthUser 수동 추출
    AuthUser authUser = extractAuthUser(principal);
    // ...
}

private AuthUser extractAuthUser(Principal principal) {
    if (principal instanceof JwtAuthenticationToken) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) principal;
        return (AuthUser) token.getPrincipal();
    }
    //...
    throw new CustomException(ExceptionCode.NOT_FOUND_PRINCIPAL);
}
```

### 왜 이렇게 해야 작동하는걸까요??

* **Principal은 Spring Security가 자동으로 주입** (JSON 역직렬화 시도 안함)
* **JwtAuthenticationToken에서 직접 AuthUser 추출**
* **Jackson의 역직렬화 과정을 완전히 우회**

---

### 📝 회고록

#### 배운 점
* **WebSocket STOMP 환경에서는 일반 REST API와 달리 @AuthenticationPrincipal이 예상대로 작동하지 않음**
* **메시지 컨버터의 역직렬화 대상이 어떻게 결정되는지 이해**
* **Principal 타입으로 받아 수동 추출하는 것이 더 안전한 패턴임을 학습**
* **STOMP 프로토콜의 파라미터 바인딩 메커니즘이 HTTP와 다르다는 점 인식**

#### 적용 결과
* **WebSocket 메시지 전송 정상 작동**
* **인증 정보 정확히 추출되어 로그에 사용자 ID, 이름, 권한 출력 확인**
* **이후 모든 WebSocket 엔드포인트에 동일한 패턴 적용**

#### 개선 가능한 부분
* **extractAuthUser() 메서드를 공통 유틸 클래스로 분리하여 재사용성 향상**
* **Custom ArgumentResolver를 만들어 @AuthUser 어노테이션으로 자동 주입 구현 검토**
* **WebSocket 환경에서의 인증 처리 Best Practice 문서화**

> **WebSocket STOMP에서는 @AuthenticationPrincipal 대신 Principal을 받아 수동으로 인증 객체를 추출하는 것이 안전하고, 메시징 환경의 특수성을 고려한 인증 처리가 필요합니다.**
