# ch08-jpa — JPA와 영속성 컨텍스트 — N+1 그 흔한 함정

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.

## 1. 실생활 비유 (Why)

ORM 은 **번역기 붙은 주방**이다.
- 한국어로 "김치찌개 5인분" 시키면 주방장이 알아서 영어 레시피로 요리한다.
- 다만 주문이 **N+1 번** 나가면 주방장도 **N+1 번** 움직인다.
- 손님 한 명이 메인 1번 + 사이드 5번 따로 시키면 주방은 6번 일한다. 한 번에 시키면 한 번.

JPA 가 "편하게" 보이는 만큼, 발행되는 SQL 을 보지 않으면 **느려진 이유조차 모르게 된다**. 이 챕터는 그 SQL 을 응답에 함께 담아 좌우로 비교한다.

## 2. 진짜 현장 이야기 (War Story)

게시판 목록 페이지가 어느 날 갑자기 **200ms → 5s**.
APM 을 열어보니 같은 SELECT 가 51회. 게시글 50개 × 댓글 LAZY 로딩 = 게시글 1번 + 댓글 50번. 클래식 N+1.

원인: 뷰 템플릿에서 `post.comments.size()` 호출 한 줄. LAZY 프록시가 호출 시점에 추가 쿼리를 발행. 트래픽이 늘자 DB 가 비명. DBA 슬랙 알람으로 새벽 2시 호출.

해법은 코드 두 줄: `@EntityGraph(attributePaths = "comments")`.
하지만 그 두 줄을 적기 전에, **N+1 이 뭔지** 와 **언제 터지는지**부터 알아야 한다.

## 3. 핵심 개념 (What)

| 비유 | 개념 | 구현 |
|---|---|---|
| 주방장의 작업대 | 영속성 컨텍스트 (1차 캐시) | EntityManager |
| 그릇을 다 치우기 전에 알아서 정리 | dirty checking | `@Transactional` 종료 시 flush |
| "필요할 때 갖다 줘" | LAZY 프록시 | `FetchType.LAZY` |
| "처음부터 같이 가져와" | EAGER / fetch join | `@EntityGraph`, `join fetch` |
| 메인 1번 + 사이드 N번 | **N+1 문제** | LAZY 컬렉션 반복 접근 시 발생 |

### N+1 의 정의
부모 N건을 조회 후, 각 부모마다 자식을 LAZY 초기화 → 총 **1 + N** 쿼리.
이 챕터의 시드 (게시글 5 × 댓글 3~5) 에서 LAZY 호출은 `SELECT 6+` 가 나간다.

### 세 가지 해법
1. **`@EntityGraph`** — 선언적, 가장 깔끔. (이 챕터의 메인)
2. **`join fetch`** — JPQL 로 직접. 페이징 시 메모리 페이징 경고.
3. **`hibernate.default_batch_fetch_size`** — IN 절로 묶어 N → N/B 쿼리로 감소.

## 4. 코드로 벼리기 (How)

```bash
# 1) Postgres 기동 (host 5433 → container 5432)
docker compose -f docker/docker-compose.local.yml up -d postgres

# 2) 백엔드 실행 — 시드 자동 (anvil.seed.enabled=true)
./gradlew :chapters:ch08-jpa:bootRun

# 3) 좌우 비교
curl -s localhost:8080/api/v1/posts/lazy  | jq '{mode, sqlCount, ms: .elapsedMillis}'
curl -s localhost:8080/api/v1/posts/fetch | jq '{mode, sqlCount, ms: .elapsedMillis}'
# 기대: lazy 의 sqlCount >> fetch 의 sqlCount

# 4) 게시글/댓글 작성
curl -s -XPOST localhost:8080/api/v1/posts \
  -H 'content-type: application/json' \
  -d '{"title":"첫 글","content":"본문"}'
curl -s -XPOST localhost:8080/api/v1/posts/1/comments \
  -H 'content-type: application/json' \
  -d '{"author":"홍길동","body":"댓글"}'
```

### 핵심 코드 (백엔드)
- `domain/Post.java`, `Comment.java` — JPA Entity (record 가 아닌 **클래스**)
- `repository/PostRepository.java` — `findAll()` (LAZY, N+1 유발) vs `findAllWithComments()` (`@EntityGraph`)
- `service/PostService.java` — 두 경로를 `@Transactional` 안에서 DTO 매핑까지 마침
- `config/SqlCapture*.java` — Hibernate `StatementInspector` 로 발행된 SQL 을 ThreadLocal 에 모음
- `controller/PostController.java` — 응답 봉투에 `sqlLogs` 동봉

## 5. 시각화 (See)

`frontend/index.html` 에서 좌(LAZY 火) / 우(FETCH 木) 버튼을 누르면 발행된 SQL 이 줄번호와 함께 펼쳐진다. 상단 배지가 쿼리 수와 소요 시간을 비교.

```bash
python3 -m http.server 5173 --directory chapters/ch08-jpa/frontend
# → http://localhost:5173
```

체험 시나리오:
1. 좌측 "LAZY" 클릭 → 6+개 SELECT 가 줄지어 등장
2. 우측 "FETCH" 클릭 → 단 1개 SELECT (LEFT JOIN 포함)
3. 게시글/댓글 작성 후 다시 좌/우 호출 → 쿼리 수 차이 더 명확

## 6. 실무 체크리스트 (ISMS-P / 성능)
- [ ] fetch 전략 결정 근거를 코드 주석으로 남겼는가 (왜 LAZY/EAGER 인가)
- [ ] `open-in-view` 는 운영 환경에서 **false** (LAZY 누수 차단)
- [ ] FK 컬럼에 인덱스가 있는가 (`@Index` 또는 마이그레이션)
- [ ] `@Transactional` 의 범위가 비즈니스 단위로 최소화되어 있는가
- [ ] 양방향 관계는 **동기화 편의 메서드** 로만 연결하는가 (`addComment`)
- [ ] Entity 를 API 응답에 직접 노출하지 않는가 (DTO 프로젝션)
- [ ] `ddl-auto` 는 운영에서 절대 `create`/`create-drop`/`update` 아닌가 (Flyway/Liquibase 사용)
- [ ] `toString/equals/hashCode` 가 양방향 컬렉션을 포함하지 않는가 (StackOverflow 방지)

## 7. 흔한 실수 & 디버깅

1. **`toString` 양방향 순환 → StackOverflowError**
   IDE 가 자동 생성한 `toString()` 이 컬렉션 포함 → `Post.toString()` ↔ `Comment.toString()` 무한 루프. **id 만** 출력.

2. **`open-in-view: false` 인데 컨트롤러에서 LAZY 접근 → LazyInitializationException**
   서비스 트랜잭션 안에서 LAZY 초기화까지 끝내고 DTO 로 반환해야 한다. 본 챕터 `listLazy()` 가 의도적으로 트랜잭션 안에서 N+1 을 발생시키는 이유.

3. **`@Transactional` 누락된 컨트롤러에서 repository 직접 호출**
   읽기는 동작하지만 LAZY 접근 시 즉시 예외. 트랜잭션 경계는 **서비스 계층** 에.

4. **`save()` 직후 id 가 null**
   `@GeneratedValue(strategy = SEQUENCE)` 일 때 flush 전까지 id 가 null. `IDENTITY` 는 즉시 INSERT 라 id 채워짐. 본 챕터는 학습용으로 `IDENTITY` 사용.

5. **`@OneToMany join fetch` + 페이징 시 메모리 페이징 경고**
   `LIMIT` 절이 사라지고 메모리에서 자름. `@EntityGraph + distinct` 또는 두 단계 쿼리로 회피.

6. **`StatementInspector` 를 Spring 빈으로 주입 시도 → 동작 X**
   Hibernate 가 직접 `new` 하므로 빈 주입 불가. `HibernateInspectorConfig` 가 **static bridge** 로 우회. (운영 코드면 datasource-proxy / p6spy.)

7. **N+1 을 "느낌" 으로 추적**
   eyeball 로 못 본다. `show_sql=true` + 줄 카운트, 또는 statement inspector / datasource-proxy 로 **숫자** 를 봐야 함.

## 8. 더 깊이 (선택)

- **다음 챕터 ch09-querydsl** — 타입 안전 동적 쿼리로 JPQL 한계 보완
- **2차 캐시** — Hibernate `@Cacheable` + EhCache/Redis (트래픽 패턴 따라 신중히)
- **DTO 프로젝션** — `interface Projection` / `JPQL constructor expression` / Spring Data `Window` API
- **batch_size** — `hibernate.default_batch_fetch_size` 로 IN 묶음 fetch
- **운영 SQL 로깅** — datasource-proxy, p6spy (포맷팅 + 슬로우 쿼리 임계치)
- Vlad Mihalcea, *High-Performance Java Persistence* — JPA/Hibernate 성능의 정석
- 공식 문서: [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/), [Hibernate 6 User Guide](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html)
