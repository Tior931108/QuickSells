# <img src="https://velog.velcdn.com/images/fluxing/post/822270e7-390d-482a-8177-3fed60bb384c/image.png" width="40" height="40" style="vertical-align: middle;"> Quick-Sells : 온라인 전당포 플랫폼
<p align="center">
  <img src="https://velog.velcdn.com/images/fluxing/post/730252b9-d9c8-4062-a6fa-eb29b783830d/image.png" width="700" alt="QuickSell Logo">
  <br>


## 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술적 개요](#2-기술적-개요)
3. [서비스 흐름](#3-서비스-흐름)
4. [기술적 도전과 해결](#4-기술적-도전과-해결)
5. [로드맵](#5-로드맵)
6. [감사의 글](#6-감사의-글)
7. [문의처](#7-문의처)
8. [팀원 소개 및 소감](#8-팀원-소개-및-소감)

---

## [1] 프로젝트 개요

**Quick-Sells**는 급전이 필요한 사용자가 자신의 물건을 담보로 맡기거나 즉시 판매할 수 있는 **온라인 전당포 & 실시간 경매 플랫폼**입니다. 기존 중고거래의 느린 판매 속도와 전당포의 접근성 문제를 해결하기 위해, 감정사 감정가 제안과 실시간 경매 시스템을 결합하여 가치 있는 물건을 가장 빠르고 합리적인 가격에 현금화할 수 있는 서비스를 지향합니다.

### 🎯 기획 의도 및 목표
* **빠른 현금화:** 감정 후 즉시 판매 시스템을 통해 최적의 가격으로 즉시 거래 성사
* **신뢰 기반 거래:** 투명한 감정 체계와 안전 결제 시스템을 통한 온라인 전당포 기능 구현
* **사용자 편의성:** 번거로운 등록 과정을 최소화하고 직관적인 입찰 경험 제공

### ✨ 주요 핵심 기능
* **실시간 역경매 & 경매:** 구매자들 간의 경쟁을 통해 상품의 가치를 극대화하는 실시간 입찰 시스템
* **감정사 시세 감정:** 이미지 분석을 통해 물건의 상태를 파악하고 적정 시작가  산출
* **안전 결제 연동:** 토스페이먼츠 등 간편 결제를 통한 신뢰도 높은 거래 환경 조성


---

## [2] 기술적 개요

### 핵심 기술 스택 (Tech Stack)
<p align="left">
  <img src="https://img.shields.io/badge/Java_17-ED8A19?style=for-the-badge&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
  <img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white">
</p>

### 상세 기술 명세 (Detailed Stack)

| 분류 | 기술 이름 |
| :--- | :--- |
| **Language & IDE** | Java 17, IntelliJ IDEA |
| **Database** | MySQL, Redis |
| **Framework & Library** | Spring Boot, Spring Data JPA, Spring WebClient, QueryDSL, Lombok |
| **Security & Payment** | JWT, Spring Security, OpenAPI, Toss Payments, Google OAuth 2.0 |
| **Infra (AWS)** | EC2, ECR, RDS, S3, ALB, NAT Gateway, Internet Gateway, IAM, Route 53 |
| **Monitoring & Log** | Prometheus, Grafana, ELK Stack (Elasticsearch, Logstash, Kibana), Filebeat |
| **CI/CD & Test** | Docker, GitHub Actions, JUnit5, Mockito, RESTDocs, Postman, Swagger |
| **Design & Collab** | ERD Cloud, Figma, draw.io, Lucidchart, Excalidraw, Slack, Notion, Zep |

### 시스템 아키텍처 (System Architecture)
> 전체적인 클라우드 인프라와 서비스 구성도입니다.


  ![](https://velog.velcdn.com/images/fluxing/post/8fed6684-ad56-436c-87a4-b2f673423c04/image.png)


* **AWS 클라우드 환경:** ALB를 통한 부하 분산 및 퍼블릭/프라이빗 서브넷 분리로 보안 강화
* **데이터 관리:** MySQL(Main DB)과 Redis(Cache/Session)의 역할 분담

### CI/CD 파이프라인 (Deployment)
> GitHub Actions와 Docker를 이용한 자동화 배포 구조입니다.

![](https://velog.velcdn.com/images/fluxing/post/f026c695-0278-4d3a-82ff-58e26312e195/image.png)

* **자동화 프로세스:** 코드 Push 시 테스트 → 빌드 → Docker Image 생성 → ECR 업로드 → EC2 배포 완료
* **모니터링:** ELK 그리고 Prometheus와 Grafana를 연동하여 서버 상태 실시간 관제

---

## [3] 서비스 흐름
> 사용자의 서비스 진입부터 경매 낙찰 및 거래 완료까지의 전체 프로세스입니다.

![](https://velog.velcdn.com/images/fluxing/post/798616e2-df89-4f30-86c9-da8f26b54521/image.png)

### 🔄 주요 프로세스 요약
* **회원가입 및 인증:** 일반/소셜 로그인을 통한 서비스 진입 및 권한 관리
* **상품 등록 및 감정:** 감정 신청을 통한 물건 가치 산정 (관리자/감정가 개입)
* **경매 및 입찰:** 포인트 충전 후 실시간 경매 입찰 참여 및 낙찰 프로세스
* **거래 및 채팅:** 낙찰 성공 시 구매자와 판매자 간 1:1 채팅을 통한 최종 거래 완료
* **마이페이지:** 내 관심 목록, 경매 내역, 포인트 입출금 관리

---

## [4] 기술적 도전과 해결

> 프로젝트 개발 과정에서 직면한 문제와 해결 과정을 정리했습니다.  
> 각 항목을 클릭하면 상세 문서를 확인할 수 있습니다.


### 1️⃣ 기술적 의사결정 (Technical Decisions)

> 특정 기술을 선택한 이유와 대안 비교, 설계 고민을 정리했습니다.

- ⚖️ [**PointWallet 낙관적 락(Optimistic Lock) 도입 결정**](https://github.com/Tior931108/QuickSells/blob/feat/readme-cjh/docs/tech_decision_wallet_optimistic_lock.md)

- ⚖️ [**실시간 채팅 시스템 WebSocket STOMP 도입**](https://github.com/Tior931108/QuickSells/blob/feat/readme-cjh/docs/tech_decision_websocket_stomp.md)

- ⚖️ [**경매 입찰 동시성 제어를 위한 Lock 설계**](https://github.com/Tior931108/QuickSells/blob/feat/readme-cjh/docs/concurrency_control_redisson_lock.md)

---

### 2️⃣ 성능 개선 (Performance Tuning)

> 조회 성능, 응답 속도, 쿼리 최적화를 통해 사용자 경험을 개선한 사례입니다.

- 🚀 [**QueryDSL Fetch Join을 통한 N+1 문제 해결 및 조회 성능 20배 개선**](https://github.com/Tior931108/QuickSells/blob/feat/readme-cjh/docs/querydsl_performance_improvement.md)

- 🚀 [**채팅방 조회 및 메시지 전송 시 N+1 쿼리 문제 최적화**](https://github.com/Tior931108/QuickSells/blob/feat/readme-cjh/docs/chat_performance_optimization.md)

---

### 3️⃣ 트러블슈팅 (Troubleshooting)

> 개발 및 운영 중 발생한 실제 장애 해결 사례입니다.

- ✅ [**결제 승인 멱등성 보장으로 중복 적립 방지**](https://github.com/Tior931108/QuickSells/blob/feat/readme-cjh/docs/payment_idempotency_protection.md)

- ✅ [**Redis 기반 상품 검색어 중복 카운트 방지**](https://github.com/Tior931108/QuickSells/blob/feat/readme-cjh/docs/redis_search_deduplication.md)

- ✅ [**WebSocket 메시지 전송 시 Jackson 역직렬화 에러 해결**](https://github.com/Tior931108/QuickSells/blob/feat/readme-cjh/docs/websocket_deserialization_troubleshooting.md)

---

## [5] 로드맵 ❌(수정 필요)
💡 **Quick-Sells**의 현재 진행 상황과 향후 업데이트 계획입니다.

| 마일스톤 | 예정 일정 | 상세 내용 | 상태 |
| :--- | :--- | :--- | :--- |
| **v1.0.0 (Launch)** | 2026년 2월 | 핵심 기능(AI 등록, 실시간 채팅, 물건 검색) 정식 런칭 | ✅ 완료 |
| **v1.1.0 (UI/UX)** | 2026년 4월 | 다크모드 지원 및 사용자 맞춤형 추천 알고리즘 고도화 | 🔄 진행중 |
| **v2.0.0 (Payment)** | 2026년 6월 | 통합 간편 결제(카드/페이) 연동 및 자체 안전결제 서비스 도입 | 📅 예정 |
| **v2.1.0 (Mobile)** | 2026년 8월 | iOS/Android 전용 네이티브 앱 출시 및 푸시 알림 강화 | 📅 예정 |

---

## [6] 감사의 글 
💡 이 프로젝트의 완성도를 높여준 핵심 기술 스택과 오픈소스 커뮤니티에 감사를 표합니다.

### 🏗️ **Core Framework & Library**
* **[Java 17](https://www.oracle.com/java/technologies/downloads/#java17)** - 안정적인 애플리케이션 실행 환경 제공
* **[Spring Boot](https://spring.io/projects/spring-boot)** - 견고하고 확장 가능한 백엔드 환경 제공
* **[Redis](https://redis.io/)** - 고성능 인 메모리 데이터 구조 저장소 및 캐싱 활용
* **[Docker](https://www.docker.com/)** - 컨테이너 기반의 일관된 개발 및 배포 환경 보장

### 📊 **Monitoring & Analysis**
* [Prometheus](https://prometheus.io/) & [Grafana](https://grafana.com/) - 실시간 시스템 메트릭 수집 및 시각화
* [ELK Stack (Elasticsearch, Logstash, Kibana)](https://www.elastic.co/kr/elastic-stack) - 대규모 로그 수집 및 분석 아키텍처 구축
* [Filebeat](https://www.elastic.co/kr/beats/filebeat) - 효율적인 로그 데이터 전송 및 정제

### 🔒 **Security & Infrastructure**
* [Spring Security](https://spring.io/projects/spring-security) & [JWT](https://jwt.io/) - 안전한 인증 및 권한 관리 체계 구축
* [AWS Cloud](https://aws.amazon.com/) - EC2, RDS, S3, Route 53 등 안정적인 인프라 환경 제공

### 🎨 **Design & Communication**
* [Figma](https://www.figma.com/) - 사용자 경험(UX) 및 인터페이스(UI) 상세 설계
* [Lucidchart](https://www.lucidchart.com/) - 협업 중심의 다이어그램 및 순서도 설계
* [Excalidraw](https://excalidraw.com/) - 직관적인 핸드드로잉 스타일의 와이어프레임 및 아이디어 스케치
* [Slack](https://slack.com/) & [ZEP](https://zep.us/) - 효율적인 팀 협업 및 실시간 커뮤니케이션 환경 제공

---
**Special Thanks to Open Source Community** 이 프로젝트는 전 세계 수많은 개발자들의 헌신으로 만들어진 오픈소스 라이브러리를 기반으로 합니다. 건강한 오픈소스 생태계를 지향하며, 저희 팀의 고민이 담긴 결과물을 **검토해 주셔서** 다시 한번 감사드립니다.

## [7] 문의처 ❌(수정 필요)
📧 **이메일:** your-email@example.com
📋 **GitHub Issues:** [Issue 등록하기](https://github.com/your-id/quick-sells/issues)

프로젝트 검토 중 궁금한 점이 있으시면 위 이메일로 연락 부탁드립니다. 최대한 빠르게 답변해 드리겠습니다!

---

## 👥 [8] 팀원 소개 및 소감

| <img src="docs/images/profile_1.png" width="150"> | <img src="docs/images/profile_2.png" width="150"> | <img src="docs/images/profile_3.png" width="150"> | <img src="docs/images/profile_4.png" width="150"> | <img src="docs/images/profile_5.png" width="150"> |
|:-------------------------------------------------:|:-------------------------------------------------:|:-------------------------------------------------:|:-------------------------------------------------:|:-------------------------------------------------:|
|                      **정용준**                      |                      **이용준**                      |                      **고아람**                      |                      **이청운**                      |                      **최정혁**                      |
|    [Tior931108](https://github.com/Tior931108)    |      [d0ngx2-2](https://github.com/d0ngx2-2)      |      [aram0117](https://github.com/aram0117)      |  [Leechungwoon](https://github.com/Leechungwoon)  | [jhyeok-design](https://github.com/jhyeok-design) |
|                       "소감"                        |                       "소감"                        |                       "소감"                        |                       "소감"                        |                       "소감"                        |



