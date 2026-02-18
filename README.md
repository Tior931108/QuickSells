# <img src="https://velog.velcdn.com/images/fluxing/post/822270e7-390d-482a-8177-3fed60bb384c/image.png" width="40" height="40" style="vertical-align: middle;"> Quick-Sells : 온라인 전당포 플랫폼
<p align="center">
  <img src="https://velog.velcdn.com/images/fluxing/post/730252b9-d9c8-4062-a6fa-eb29b783830d/image.png" width="700" alt="QuickSell Logo">
  <br>


## 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술적 개요](#2-기술적-개요)
3. [시작하기](#3-시작하기)
4. [사용자 가이드](#4-사용자-가이드)
5. [트러블슈팅](#5-트러블슈팅)
6. [로드맵](#6-로드맵)
7. [기여하기](#7-기여하기)
8. [감사의 글](#8-감사의-글)
9. [문의처](#9-문의처)
10. [라이선스](#10-라이선스)

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

## [3] 서비스 흐름 (Service Flow)
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
> 프로젝트 개발 과정에서 직면한 문제들과 이를 해결하기 위한 기술적 고민의 기록입니다. 각 항목을 클릭하면 상세한 해결 과정을 확인할 수 있습니다.

### ⚖️ 기술적 의사결정 (Technical Decision)
특정 기술을 왜 선택했는지, 대안은 무엇이었는지에 대한 아키텍처적 고민입니다.

### 🚀 성능 개선 (Performance Tuning)
시스템의 응답 속도, 자원 사용량 등 사용자 경험을 향상시킨 기록입니다.
* [QueryDSL Fetch Join을 통한 N+1 문제 해결 및 조회 성능 20배 개선](./docs/performance/querydsl_performance_improvement.md)

### 🛠️ 트러블슈팅 (Troubleshooting)
운영 및 개발 중 발생한 예기치 못한 버그와 기술적 장애를 해결한 사례입니다.

---

## [6] 로드맵
💡 **Quick-Sells**의 현재 진행 상황과 향후 업데이트 계획입니다.

| 마일스톤 | 예정 일정 | 상세 내용 | 상태 |
| :--- | :--- | :--- | :--- |
| **v1.0.0 (Launch)** | 2026년 2월 | 핵심 기능(AI 등록, 실시간 채팅, 물건 검색) 정식 런칭 | ✅ 완료 |
| **v1.1.0 (UI/UX)** | 2026년 4월 | 다크모드 지원 및 사용자 맞춤형 추천 알고리즘 고도화 | 🔄 진행중 |
| **v2.0.0 (Payment)** | 2026년 6월 | 통합 간편 결제(카드/페이) 연동 및 자체 안전결제 서비스 도입 | 📅 예정 |
| **v2.1.0 (Mobile)** | 2026년 8월 | iOS/Android 전용 네이티브 앱 출시 및 푸시 알림 강화 | 📅 예정 |

---

## [7] 기여하기
✨(기능 추가) / 🐞(버그 수정) / 📄(문서화) / 🔨(리팩토링) / ⚡(성능 최적화)
이 프로젝트에 기여하고 싶으신 분은 언제든 Pull Request를 보내주세요!
1. 프로젝트를 **Fork** 합니다.
2. 새 **Feature Branch**를 생성합니다 (`git checkout -b feature/AmazingFeature`).
3. 변경 사항을 **Commit** 합니다 (`git commit -m 'Add some AmazingFeature'`).
4. Branch에 **Push** 합니다 (`git push origin feature/AmazingFeature`).
5. **Open a Pull Request**를 통해 제안해 주세요.

🙌 **기여해주신 분들:** [기여자 명단 보기](./CONTRIBUTORS.md)

---

## [8] 감사의 글
💡 이 프로젝트를 개발하는 데 영감을 주거나 도움을 준 리소스들입니다.

* [Shields.io](https://shields.io/) - 프로젝트 가독성을 높여주는 배지 아이콘 제공
* [Spring Initializr](https://start.spring.io/) - 빠르고 견고한 백엔드 구조 생성 도움
* [Flaticon](https://www.flaticon.com/) - 직관적인 사용자 경험을 위한 서비스 아이콘 소스
* [Draw.io](https://app.diagrams.net/) - 시스템 아키텍처 설계를 위한 다이어그램 도구
* **Open Source Community** - 이 프로젝트에 사용된 수많은 라이브러리의 개발자분들께 감사드립니다.

---

## [9] 문의처
📧 **이메일:** your-email@example.com
📋 **GitHub Issues:** [Issue 등록하기](https://github.com/your-id/quick-sells/issues)

프로젝트 이용 중 궁금한 점이 있거나 제휴 제안이 있으시면 위 이메일로 연락 부탁드립니다. 최대한 빠르게 답변해 드리겠습니다!

---

## [10] 라이선스
이 프로젝트는 **MIT License**를 따릅니다.
상업적 이용, 수정 및 배포가 가능하나, 저작권 및 라이선스 고지 사항을 포함해야 합니다. 자세한 내용은 [LICENSE](./LICENSE) 파일을 확인하세요.

Copyright (c) 2026 Quick-Sells Team