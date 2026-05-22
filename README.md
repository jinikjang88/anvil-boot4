# anvil-boot4

> 대장간(DevSmith) 백엔드 — Spring Boot 4 + Java 25 학습 레포지토리.
> 에북 *"대장간 백엔드"* 의 라이브 부록.

## 빠른 시작

```bash
# 1) 로컬 의존성 (Postgres / Redis / LocalStack) 기동
docker compose -f docker/docker-compose.local.yml up -d

# 2) 챕터 백엔드 실행 (예: ch01)
./gradlew :chapters:ch01-foundation:bootRun

# 3) 챕터 프론트엔드 (정적)
python3 -m http.server 5173 --directory chapters/ch01-foundation/frontend
```

## 챕터 구조

각 챕터는 독립 실행 가능하다.

```
chapters/chXX-<topic>/
├── README.md   # 8 섹션 (Why / War Story / What / How / See / Checklist / Pitfalls / Deeper)
├── backend/    # Spring Boot Gradle 서브모듈
└── frontend/   # 정적 HTML/CSS/JS + CDN (Tailwind / D3 / Chart.js)
```

새 챕터:

```bash
./scripts/new-chapter.sh <id> <slug> "<title>"
# 예: ./scripts/new-chapter.sh 01 foundation "Spring Boot 4의 세계"
```

## 16 챕터 로드맵

### Part 1. 기초 다지기 (모루에 올리기)
| 챕터 | 제목 |
|---|---|
| ch01-foundation       | Spring Boot 4의 세계 — 왜 또 새 버전인가 |
| ch02-java25-features  | Java 25 신문법 한 입씩 |
| ch03-di               | 의존성 주입 — 왜 `new` 쓰면 안 되나 |
| ch04-config           | 설정의 기술 — local / dev / prod |

### Part 2. 웹 계층 (망치질 시작)
| 챕터 | 제목 |
|---|---|
| ch05-rest-api  | REST API 설계 — 메뉴판처럼 만들기 |
| ch06-validation | 검증과 예외 — 손님의 진상 요청 막기 |
| ch07-security  | 보안 기초 — 가게 문에 자물쇠 채우기 |

### Part 3. 데이터 계층 (담금질)
| 챕터 | 제목 |
|---|---|
| ch08-jpa       | JPA 한입에 — ORM 이 왜 필요한가 |
| ch09-querydsl  | QueryDSL — 동적 쿼리 우아하게 |
| **ch09b-jooq** | **jOOQ — SQL-First 비교 세부챕터** |
| ch10-transaction | 트랜잭션 — 송금의 약속 |

### Part 4. 확장 (벼림의 정수)
| 챕터 | 제목 |
|---|---|
| ch11-cache | 캐시 — 자주 보는 메뉴는 외워두기 |
| ch12-async | 비동기와 이벤트 — 일 떠넘기기 |
| ch13-kafka | Kafka — 줄 세우는 기술 |

### Part 5. 운영 (검수와 마무리)
| 챕터 | 제목 |
|---|---|
| ch14-testing       | 테스트 — 망치질 전에 두드려보기 |
| ch15-observability | 관측성 — 가게 CCTV 달기 |
| ch16-deploy        | 배포 — 가게 오픈 준비 |

## 스택

- Java 25 LTS · Spring Boot 4.x · Gradle 8.x (Kotlin DSL · Version Catalog)
- JUnit 5 · AssertJ · Testcontainers
- PostgreSQL 16 · Redis 7 · Kafka · LocalStack (챕터별 선택)
- 프론트: HTML/CSS/JS + CDN (Tailwind · D3.js · Chart.js)

## 통합 인덱스

`docs/index.html` 은 16 챕터 데모를 한 곳에서 둘러볼 수 있는 통합 페이지다 (GitHub Pages 배포 대상).

## 컨벤션

자세한 코드/문서 컨벤션은 [`.claude/CLAUDE.md`](.claude/CLAUDE.md) 참조.
