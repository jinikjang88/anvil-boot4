# ch09b-jooq — jOOQ — SQL-First 비교 세부챕터

> ch09-querydsl 의 자매편. 같은 게시판 도메인을 **SQL-First** 로 다시 짠다.
> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.

## 1. 실생활 비유 (Why)

ORM (JPA/QueryDSL) 은 **번역기 붙은 주방**이다 (ch08 비유). 손님이 한국어로 시키면 주방장이 알아서 영어 레시피로 요리.
- 일반 식사라면 편하다.
- 그런데 손님이 "와인 페어링 + 비건 옵션 + 알레르기 회피 + 분자요리 기법 + 한 접시에 인분 다르게" 같이 복합 주문을 하면? 번역기로는 표현조차 못 한다.

jOOQ 는 **주방장과 직접 영어로 대화하는 손님**이다.
- 메뉴판(스키마)이 코드로 자동 생성되어 손님(개발자) 손에 들어옴.
- 손님은 영어(SQL)를 직접 말한다 — 단, 컴파일러가 옆에서 문법을 봐준다.
- "ROW_NUMBER OVER PARTITION BY" 같은 셰프의 비기를 손님이 그대로 요청 가능.

## 2. 진짜 현장 이야기 (War Story)

분석팀에서 **"카테고리별 상위 5개 인기 글 + 작성자별 누적 조회수 + 댓글 트리 펼침"** 보고서 요청.
- JPA 로 try: `findTop5ByCategoryOrderByViewsDesc` 를 카테고리 개수만큼? 카테고리 50개 = 50번 라운드 트립.
- QueryDSL 로 try: 윈도우 함수는 표준 미지원. native query 로 우회 → 결과 매핑 수동 → 타입 세이프 깨짐.
- 결국 `JdbcTemplate` 으로 String SQL 쓰고 `RowMapper` 수동.
- 어느 날 컬럼명을 바꿨는데 RowMapper 가 NPE — 컴파일러가 못 잡아 운영에서 발견.

jOOQ 로 옮긴 뒤:
- 윈도우 함수가 한 줄. `rowNumber().over().partitionBy(CATEGORY).orderBy(VIEWS.desc())`.
- 컬럼명을 바꾸면 Q클래스가 재생성 → 사용처 모두 컴파일 에러.
- 같은 트랜잭션 안에서 JPA 도 같이 쓴다 (도메인 CRUD 는 JPA, 보고서는 jOOQ).

## 3. 핵심 개념 (What)

| 비유 | 개념 | 구현 |
|---|---|---|
| 영어로 주문 | SQL 그대로 | `dsl.select(...).from(...).where(...)` |
| 메뉴판 자동 인쇄 | 코드 생성 | nu.studer.jooq 플러그인 (Full 모드) |
| 카운터에서 직접 정산 | DSLContext | 트랜잭션은 Spring `@Transactional` |
| 셰프 전용 기법 | 윈도우 / CTE / Vendor SQL | jOOQ 일급 표현 |

### 본 챕터의 5가지 시연

| API | 기능 | SQL 의 핵심 | QueryDSL 로는? |
|---|---|---|---|
| `GET /jooq/search` | 동적 검색 (ch09 비교군) | `Condition + noCondition()` | 가능 — 비등비등 |
| `GET /jooq/ranking?topN=N` | 작성자별 상위 N | `ROW_NUMBER() OVER (PARTITION BY ...)` | **표준 미지원** |
| `GET /jooq/tree/{postId}` | 댓글 트리 (depth 포함) | `WITH RECURSIVE` | **미지원** |
| `POST /jooq/upsert` | (title, author) upsert | `INSERT … ON CONFLICT DO UPDATE` | **미지원** |
| `POST /jooq/bulk-comments` | N rows 한 SQL | `INSERT … VALUES (?,?), (?,?), …` | JPA saveAll 은 N 라운드 |

### Lite 모드 (이 챕터) vs Full 모드 (정공법)
| | Lite | Full |
|---|---|---|
| 코드 생성 | 없음 (`DSL.table/field`) | nu.studer.jooq 플러그인 + Flyway |
| 타입 세이프 | 컬럼명/타입 일부 (수동 정의) | 완전 (스키마 변경 → 컴파일 에러) |
| 빌드 복잡도 | 낮음 (단순 의존성) | 중간 (Docker / buildSrc 추가) |
| 학습 비용 | 낮음 | 중간 |
| 실무 권장 | PoC, 짧은 코드 | 운영, 큰 팀 |

이 챕터는 **Lite** — codegen 인프라를 빼고 jOOQ 의 SQL 표현력에 집중. Full 모드 워크플로는 §8 코드 스니펫.

## 4. 코드로 벼리기 (How)

```bash
# 1) Postgres
docker compose -f docker/docker-compose.local.yml up -d postgres

# 2) 백엔드 (스키마 자동 DROP+CREATE + 시드 자동)
./gradlew :chapters:ch09b-jooq:bootRun

# 3) 다섯 가지 시연
curl -s 'localhost:8080/api/v1/jooq/search?author=민지'        | jq '{sqlCount, total: .totalElements}'
curl -s 'localhost:8080/api/v1/jooq/ranking?topN=2'             | jq '{sqlCount, data: .data | length}'
curl -s 'localhost:8080/api/v1/jooq/tree/2'                     | jq '{sqlCount, depth: .maxDepth, total: .totalNodes}'
curl -s -XPOST 'localhost:8080/api/v1/jooq/upsert' \
     -H 'content-type: application/json' \
     -d '{"title":"u-test","content":"v1","author":"민지"}' | jq '{sqlCount, id}'
# 같은 키로 다시 → 본문만 업데이트
curl -s -XPOST 'localhost:8080/api/v1/jooq/upsert' \
     -H 'content-type: application/json' \
     -d '{"title":"u-test","content":"v2","author":"민지"}' | jq '{sqlCount, id}'
```

### 핵심 코드

| 파일 | 역할 |
|---|---|
| `schema/Tables.java` | Lite 모드의 테이블/필드 메타 (codegen 대체) |
| `config/SchemaInitializer.java` | 부팅 시 DDL 직접 실행 (Flyway 대체) |
| `config/JooqSqlListener.java` | jOOQ ExecuteListener → SQL 캡처 (ch08 의 Hibernate Inspector 대응물) |
| `repository/JooqPostSearchRepository.java` | 검색 — `DSL.and(conditions)` + `noCondition()` |
| `repository/JooqRankingRepository.java` | **`rowNumber().over().partitionBy(...)`** — nested select 패턴 |
| `repository/JooqCommentTreeRepository.java` | **`withRecursive(name).as(anchor.unionAll(recursive))`** |
| `repository/JooqUpsertRepository.java` | **`.onConflict(...).doUpdate().set(col, DSL.excluded(col))`** |
| `repository/JooqBulkInsertRepository.java` | **`.values(...).values(...)…`** 체이닝 |

## 5. 시각화 (See)

`frontend/index.html` — 5가지 데모 카드 + 비교 표.

```bash
python3 -m http.server 5175 --directory chapters/ch09b-jooq/frontend
# → http://localhost:5175
```

체험 시나리오:
1. ① 검색 — ch09 와 같은 SQL 이 나옴. 검색 영역은 두 도구 막상막하.
2. ② 랭킹 topN=2 — SQL 1개에 `ROW_NUMBER ... PARTITION BY` 보임.
3. ③ 트리 postId=2 — SQL 에 `WITH RECURSIVE` 보임. 들여쓰기로 depth 시각화.
4. ④ Upsert — 같은 (title, author) 로 두 번 호출 → 두 번째는 `id` 동일, content 만 갱신.
5. ⑤ Bulk INSERT 500 rows — `sqlCount = 1` (한 SQL).

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)
- [ ] 동적 검색에서 `noCondition()` 으로 명시적 no-op 처리 (= 전체 조회) 가 의도된 것인가
- [ ] `onConflict` 키는 unique constraint 와 일치하는가 (PostgreSQL 은 인덱스 매칭 필요)
- [ ] 윈도우 함수의 `PARTITION BY` 컬럼에 인덱스가 있는가
- [ ] `WITH RECURSIVE` 의 종료 조건 / 깊이 제한이 있는가 (무한 루프 방지 — 운영에선 `LIMIT 100` 같은 안전 장치)
- [ ] Bulk INSERT 의 row 수 상한 (네트워크 페이로드 / 메모리 / pgbouncer 한계)
- [ ] jOOQ + JPA 혼용 시 트랜잭션 경계가 일관된가 (둘 다 같은 PlatformTransactionManager 공유 — Spring Boot 가 자동)
- [ ] codegen (Full 모드) 시 생성된 클래스는 VCS 에서 제외 (build 산출물 처리)
- [ ] Lite 모드의 컬럼명 문자열은 단일 출처(`Tables.java`)로 강제하는가

## 7. 흔한 실수 & 디버깅

1. **`DSL.excluded(field)` 가 안 됨**
   jOOQ 3.13+ 부터 제공. Spring Boot 4 BOM 의 jOOQ 가 그 이상인지 확인. 미만이면 `field("EXCLUDED." + col, type)` 또는 stringTemplate.

2. **윈도우 함수에 `.qualify(...)` 사용 → PostgreSQL 미지원**
   QUALIFY 는 Snowflake/Teradata/Trino 전용. PostgreSQL 은 nested select 로 우회. jOOQ 가 자동 변환해주긴 하지만 학습용으로는 명시적으로 `asTable(...)` 후 다시 select 가 명확.

3. **`InsertValuesStep4<CAP#1, ...>` 컴파일 에러**
   `Tables.COMMENTS` 가 `Table<?>` 인데 `InsertValuesStep4` 의 첫 타입 파라미터에 와일드카드가 캡처되어 incompatible types. → `var` 사용 또는 `Table<Record>` 로 명시.

4. **부팅 시 `relation "posts" does not exist`**
   `SchemaInitializer` 의 `@Order(HIGHEST_PRECEDENCE)` 가 DataSeeder 보다 먼저 실행되도록. 둘 다 `ApplicationRunner` 면 명시적 `@Order` 없이는 순서 모호.

5. **jOOQ SQL 로그가 한 줄도 안 잡힘**
   `ExecuteListenerProvider` 빈을 Boot 가 자동으로 jOOQ Configuration 에 꽂아주는지 확인. 직접 `DefaultConfiguration` 을 빈으로 노출하면 자동 통합이 깨질 수 있음 — Boot 기본 설정에 맡기자.

6. **`fetchOne(0, Integer.class)` 가 null 반환**
   `selectCount()` 는 항상 한 행을 반환 — 그러나 jOOQ 의 nullable 타입 추론 때문에 boxing NPE 가능. null-safe 변환 (`n == null ? 0 : n`).

7. **`ON CONFLICT (title, author)` 가 안 잡힘**
   conflict 키는 **존재하는 unique constraint 또는 unique index** 와 정확히 매칭되어야 한다. 본 챕터는 `uq_posts_title_author` 를 SchemaInitializer 에서 생성.

## 8. 더 깊이 — codegen 직접 체험

이 챕터의 `build.gradle.kts` 에는 `nu.studer.jooq` 플러그인이 **이미 설정**되어 있다.
단, 기본 빌드에서는 비활성화 (`-Pcodegen` 프로퍼티 필요).

### 직접 돌려보기

```bash
# 1) PostgreSQL 기동
docker compose -f docker/docker-compose.local.yml up -d postgres

# 2) 테이블 생성 (SchemaInitializer 가 자동)
./gradlew :chapters:ch09b-jooq:bootRun
# Ctrl+C 로 종료

# 3) codegen 실행 (DB 에 테이블이 있는 상태에서)
./gradlew :chapters:ch09b-jooq:generateJooq -Pcodegen

# 4) 생성 결과 확인
tree chapters/ch09b-jooq/backend/build/generated/jooq/
# com/devsmith/anvil/ch09b/generated/
# ├── tables/
# │   ├── Comments.java    ← 컬럼 정보가 타입 세이프 필드로
# │   ├── Posts.java
# │   └── records/
# │       ├── CommentsRecord.java  ← POJO
# │       └── PostsRecord.java
# ├── Tables.java           ← 자동 생성 테이블 참조
# ├── Keys.java             ← PK/FK/UK
# └── Indexes.java
```

### Lite vs codegen diff 비교

```bash
# 수동 정의 (Lite) — 컬럼명 문자열이 단일 출처이긴 하지만 DB 변경 시 수동 동기화 필요
cat chapters/ch09b-jooq/backend/src/main/java/com/devsmith/anvil/ch09b/schema/Tables.java

# 자동 생성 (Full) — DB 스키마가 바뀌면 codegen 재실행으로 즉시 반영, 깨진 곳은 컴파일 에러
cat chapters/ch09b-jooq/backend/build/generated/jooq/com/devsmith/anvil/ch09b/generated/Tables.java
```

### Flyway 마이그레이션 파일 (참고용)

`src/main/resources/db/migration/V1__init.sql` — 현재 SchemaInitializer 의 DDL 과 동일 내용.
Full 모드에서는 이 파일이 **스키마의 진실의 원천(Single Source of Truth)**.

운영 워크플로:
1. `V2__add_views_column.sql` 같이 마이그레이션 추가
2. `./gradlew flywayMigrate` → DB 에 적용
3. `./gradlew generateJooq -Pcodegen` → 코드 재생성 (변경된 컬럼이 자동 반영)
4. 기존 코드에서 삭제된 컬럼 참조 → **컴파일 에러** → 안전하게 수정

Flyway 런타임 적용까지 가려면:
- `build.gradle.kts` 에 `implementation("org.flywaydb:flyway-database-postgresql")` 추가
- `application.yml` 에 `spring.flyway.enabled=true`
- SchemaInitializer 비활성화 (`@ConditionalOnProperty(name = "anvil.schema.manual", havingValue = "true")`)

### 공식 문서
- [jOOQ Manual](https://www.jooq.org/doc/latest/manual/)
- [jOOQ + Spring Boot](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.jooq)
- [nu.studer.jooq Gradle plugin](https://github.com/etiennestuder/gradle-jooq-plugin)
- [Flyway Database Postgres](https://documentation.red-gate.com/fd/postgresql-184127574.html)
- Lukas Eder (jOOQ creator) blog — 윈도우 함수 / SQL trick 의 정석
