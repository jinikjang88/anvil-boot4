# anvil-boot4

대장간(DevSmith) 브랜드의 Spring Boot 4 + Java 25 학습 레포지토리.
에북 "대장간 백엔드"의 16챕터 커리큘럼을 실습 코드 + 인터랙티브 프론트로 구현한다.
레포 자체가 에북의 "라이브 부록" 역할을 한다.

## 스택
- Java 25 (LTS), Spring Boot 4.x, Gradle 8.x (Kotlin DSL) + Version Catalog
- JUnit 5, AssertJ, Testcontainers
- PostgreSQL 16, Redis 7, Kafka (챕터별 선택), LocalStack
- 프론트: HTML/CSS/JS + CDN (Tailwind Play / D3.js / Chart.js / htmx / Alpine.js 선택적)
- 프레임워크(React/Vue) **사용 금지** — 백엔드 학습에 집중

## 디렉터리 구조
```
anvil-boot4/
├── chapters/
│   └── chXX-<topic>/
│       ├── README.md          # 8-섹션 공통 포맷 (아래 참조)
│       ├── backend/           # Spring Boot Gradle 서브모듈
│       │   ├── build.gradle.kts
│       │   └── src/main, src/test
│       └── frontend/          # 정적 자산 (HTML/CSS/JS, CDN 라이브러리)
│           ├── index.html
│           ├── style.css
│           └── script.js
├── common/                    # (필요 시) 챕터 공통 유틸 — 챕터 간 의존은 금지
├── docs/
│   └── index.html             # 16챕터 통합 인덱스 (GitHub Pages 배포)
├── docker/docker-compose.local.yml
├── gradle/libs.versions.toml
└── buildSrc/                  # convention plugins
```

## 구조 원칙
- `chapters/chXX-*` 는 각각 **독립 빌드** 가능해야 한다 (학습자가 챕터 단위 clone/실행)
- 챕터 간 코드 의존 **금지**. 공통이 정말 필요하면 `common/` 으로 분리하고 명시
- frontend/ 는 **정적 자산만** — Gradle 빌드 대상 아님. backend/ 만 Gradle 서브모듈로 include
- backend 는 frontend 를 모르고, frontend 는 fetch 로 backend API 만 호출

## 챕터 공통 포맷 (README.md 필수 8 섹션)
모든 챕터 README 는 아래 순서로 작성한다 (학습자 예측 가능성 확보).

1. **실생활 비유 (Why)** — 왜 필요한가? 일상 예시로
2. **진짜 현장 이야기 (War Story)** — 실무에서 안 썼을 때 뭐가 터졌나
3. **핵심 개념 (What)** — 비유 → 개념 매핑
4. **코드로 벼리기 (How)** — 실행 가능한 예제 (backend/ 코드 참조)
5. **시각화 (See)** — frontend/ 데모 또는 다이어그램 링크
6. **실무 체크리스트** — ISMS-P, 성능, 보안 관점
7. **흔한 실수 & 디버깅** — "내가 겪은 삽질"
8. **더 깊이 (선택)** — 심화 자료, 공식 문서 링크

## 16챕터 로드맵

### Part 1. 기초 다지기 (모루에 올리기)
- ch01-foundation       — Spring Boot 4 의 세계, 왜 또 새 버전인가
- ch02-java25-features   — Java 25 신문법 (Virtual Thread, Pattern Matching, Records, Sealed)
- ch03-di                — 의존성 주입, 왜 new 쓰면 안 되나
- ch04-config            — 설정의 기술 (local/dev/prod, ConfigurationProperties, Secrets)

### Part 2. 웹 계층 (망치질 시작)
- ch05-rest-api          — REST API 설계, 메뉴판처럼 만들기
- ch06-validation        — 검증과 예외 (Bean Validation, Problem Details RFC 7807)
- ch07-security          — 보안 기초 (Spring Security 6+, JWT, OAuth2, 암호화)

### Part 3. 데이터 계층 (담금질)
- ch08-jpa               — JPA 한입에, 영속성 컨텍스트
- ch09-querydsl          — QueryDSL, 동적 쿼리 (타입 세이프 ORM 위의 DSL)
- ch09b-jooq             — jOOQ, SQL-First 비교 세부챕터
  - JPA/QueryDSL(ORM-First) ↔ jOOQ(SQL-First) 패러다임 차이
  - 동일 도메인을 세 방식으로 구현 → 코드/쿼리/성능/유지보수 관점 비교표
  - 언제 jOOQ 를 골라야 하는가 (복잡한 집계, 윈도우 함수, 벤더별 SQL, 코드 생성 워크플로)
  - War Story: ORM N+1 / fetch join 한계로 결국 native query 산발 → jOOQ 로 통일한 사례
- ch10-transaction       — 트랜잭션, 격리/전파/AOP 함정

### Part 4. 확장 (벼림의 정수)
- ch11-cache             — 캐시 (Redis, TTL, Stampede)
- ch12-async             — 비동기와 이벤트 (Virtual Thread Executor, @Async)
- ch13-kafka             — 메시징 (파티션, 컨슈머 그룹, 멱등성)

### Part 5. 운영 (검수와 마무리)
- ch14-testing           — 테스트 (Testcontainers, 테스트 더블, 피라미드)
- ch15-observability     — 관측성 (Micrometer, OpenTelemetry, 구조화 로깅)
- ch16-deploy            — 배포 (GraalVM Native, Docker, Actions, Blue-Green)

## 비주얼 컨셉 — 오행 컬러 (대장간 + 사주 브랜드 통합)
| 역할 | 오행 | HEX (가이드) |
|---|---|---|
| 배경       | 土 (베이지)   | `#F5EFE0` |
| 강조/경고  | 火 (빨강)     | `#C0392B` |
| 데이터/링크 | 水 (파랑)     | `#1E5F8C` |
| 성공/긍정  | 木 (초록)     | `#3B7A57` |
| 보조 텍스트 | 金 (회색)     | `#7F8C8D` |

- 모든 frontend/ 페이지는 이 팔레트를 따른다 (style.css 에 CSS 변수로 정의)
- 다이어그램(D3.js) 색상도 동일 팔레트 사용

## 프론트엔드 가이드
- 빌드 도구 없음 — 정적 파일만. `python -m http.server` 로도 열린다
- CDN 으로 가져올 라이브러리만 사용:
  - 필수: Tailwind Play CDN, D3.js, Chart.js
  - 선택: htmx (API 호출 데모), Alpine.js (간단한 상태)
- 각 챕터 frontend/index.html 은 단일 페이지로 자기 챕터 데모만 다룬다
- 통합 인덱스 `docs/index.html` 에서 16챕터 데모 링크를 모두 제공 → GitHub Pages 배포

## 코드 컨벤션
- **한글 주석** 사용 (학습 목적)
- 테스트: given-when-then 패턴, 메서드명 한글 백틱 허용 (`@Test void \`회원_가입_성공_시_200_반환\`()`)
- SOLID, Design Pattern 명시적 적용 — **어떤 패턴을 왜 썼는지 주석으로**
- 예외 처리와 엣지 케이스를 명시적으로
- 보안: SQL Injection, XSS, 개인정보 암호화 항상 고려 (ISMS-P 관점)

## 환경
- application-local.yml / application-dev.yml / application-prod.yml 분리
- 로컬 의존성은 `docker/docker-compose.local.yml` 로 일괄 기동
- AWS 의존성은 LocalStack 사용

## 작업 규칙 (클로드 코드용)
- 새 챕터 생성 시 동일 구조 복제 (`chapters/chXX-*/{backend,frontend}` + README 8섹션)
- 라이브러리 버전 추가는 반드시 `gradle/libs.versions.toml` 에 등록 후 참조
- 커밋 메시지는 Conventional Commits + 챕터 prefix: `feat(ch03): JPA 영속성 컨텍스트 예제 추가`
- 큰 변경 전에는 계획을 먼저 제시하고 승인 대기
