# ch09-querydsl — QueryDSL과 동적 쿼리

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.
> 이 챕터의 자매편: `chapters/ch09b-jooq/` — 같은 도메인을 SQL-First 로 다시 짠다.

## 1. 실생활 비유 (Why)

JPQL/Spring Data 메서드 이름은 **객실 종류가 정해진 호텔**이다.
- "스탠다드 / 디럭스 / 스위트" — 미리 정해진 옵션 안에서만 고를 수 있다.
- 손님이 "전망은 바다, 침대는 킹사이즈, 조식은 일식, 단 흡연실은 빼고…" 라고 5가지 조건을 동적으로 조합하면? 객실 종류를 32개 만들거나, 프런트가 매번 손으로 조합해 줘야 한다.

QueryDSL 은 **자유 조합 가능한 모듈 가구** 다.
- 의자, 책상, 서랍 같은 부품 (= `BooleanExpression`) 을 조립한다.
- 손님이 필요 없는 부품은 그냥 안 쓰면 (= `null` 반환) 자동으로 빠진다.
- 부품의 모양은 컴파일러가 검증 (= 타입 세이프). 못 들어가는 부품은 못 끼운다.

## 2. 진짜 현장 이야기 (War Story)

검색 API 가 처음엔 `findByAuthor(...)` 한 줄로 충분했다.
- 기획자가 "기간 필터도" → `findByAuthorAndCreatedAtBetween(...)`
- "키워드로 제목 검색도" → `findByAuthorAndCreatedAtBetweenAndTitleContaining(...)`
- "키워드는 본문도 같이" → JPQL 로 강제 이동 (`@Query`)
- "조건 5개 중 N개만 입력해도 동작해야 함" → if 문 폭발, 문자열 누적
- 어느 날 동료가 `cond.author` 의 null 체크를 빼먹어 → **전체 게시글 100만건이 반환**. 페이지 캐시 폭발, DB 비명, 새벽 호출.

원인은 단순했다 — "조건이 비어있으면 WHERE 절을 빼라" 라는 약속을 **사람이 매번 지켜야** 했던 것. 컴파일러가 도와주지 않으니 실수가 누적됐다.

QueryDSL 로 옮긴 뒤로는:
- `BooleanExpression authorEq(...)` 가 `null` 을 반환하면 `where(...)` 가 자동 무시.
- 컬럼명/엔티티명 오타는 **컴파일 에러**.
- 검색 조건이 5개에서 8개로 늘어도 메서드 하나가 한 줄 더 자라는 정도.

## 3. 핵심 개념 (What)

| 비유 | 개념 | 구현 |
|---|---|---|
| 정해진 객실 메뉴 | Spring Data 메서드 파생 | `findByAuthorAnd…` (조합 폭발) |
| 손글씨 주문서 | JPQL `@Query` 문자열 | 조건마다 if + 문자열 누적 |
| 조립 가구 (명령형) | QueryDSL **BooleanBuilder** | if → `builder.and(...)` |
| 조립 가구 (함수형) | QueryDSL **BooleanExpression** | private 메서드가 `null` 또는 표현식 |
| 부품 도면 | Q클래스 (`QPost`) | `annotationProcessor` 가 자동 생성 |

### 왜 BooleanExpression 이 권장인가
- `where(authorEq(a), createdAtGoe(f), createdAtLt(t), …)` 한 줄.
- 각 조건이 메서드로 추출되어 **재사용 가능** (count 쿼리, 다른 검색, 정책 분기 등).
- `null` 자동 무시 — "조건이 빠지면 WHERE 도 빠진다" 는 약속을 **API 가 강제**.
- 동시에 함정도 있음: 조건을 빠뜨려도 컴파일러가 모름. → **통합 테스트로 회귀 방어** 필수.

### 페이징 최적화 — `PageableExecutionUtils.getPage()`
content 가 페이지 크기보다 작으면 count 쿼리를 생략한다 (마지막 페이지). 큰 테이블에서 차이가 극적.

## 4. 코드로 벼리기 (How)

```bash
# 1) Postgres 기동 (host 5433 → container 5432)
docker compose -f docker/docker-compose.local.yml up -d postgres

# 2) 백엔드 실행 — 시드 12건 자동
./gradlew :chapters:ch09-querydsl:bootRun

# 3) 세 가지 호출 비교 — 같은 검색 조건, 다른 구현
COND='?keyword=Spring&author=준호&hasComments=true'
curl -s "localhost:8080/api/v1/posts/search/jpql${COND}"       | jq '{mode, sqlCount, total: .totalElements}'
curl -s "localhost:8080/api/v1/posts/search/builder${COND}"    | jq '{mode, sqlCount, total: .totalElements}'
curl -s "localhost:8080/api/v1/posts/search/expression${COND}" | jq '{mode, sqlCount, total: .totalElements}'

# 4) 조건 0개 — null 무시 동작 확인
curl -s "localhost:8080/api/v1/posts/search/expression" | jq '{sqlCount, total: .totalElements}'
```

### 핵심 코드 (백엔드)
- `domain/Post.java`, `Comment.java` — JPA Entity (ch08 와 동일 도메인을 재정의 — 챕터 간 의존 금지)
- `search/PostSearchCondition.java` — 검색 조건 record (5개 필드, 모두 nullable)
- `repository/PostQueryRepository.java` (interface) + `PostQueryRepositoryImpl.java` — 세 가지 스타일 한 클래스에 모음
- `config/QuerydslConfig.java` — **JPAQueryFactory 빈 등록** (이걸 빼면 부팅 실패)
- `config/SqlCapture*.java` — Hibernate StatementInspector 로 발행 SQL 캡처 (ch08 패턴 재정의)

### Q클래스 자동 생성
빌드 시 `build/generated/sources/annotationProcessor/.../QPost.java` 가 만들어진다.
- 어떻게? `annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")` 가 `@Entity` 를 보고 생성.
- Boot 4 / Hibernate 6 부터는 `jakarta` classifier 필수 — 안 붙이면 `javax.persistence.*` 임포트로 컴파일 실패.

## 5. 시각화 (See)

`frontend/index.html` — 검색 폼 + 세 호출의 SQL/결과 사이드바이사이드.

```bash
python3 -m http.server 5173 --directory chapters/ch09-querydsl/frontend
# → http://localhost:5173
```

체험 시나리오:
1. 조건 모두 비우고 "실행" → 세 호출 모두 `where` 가 없는 동일 SQL
2. `author=민지` 만 입력 → SQL 에 `where author = ?` 한 줄 추가
3. `keyword=QueryDSL` + `hasComments=true` → `where (lower(title) like ? or lower(content) like ?) and exists (…)`
4. JPQL / Builder / Expression 의 SQL 이 거의 같아야 함 — 같은 결과를 다른 코드로 만든 것

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)
- [ ] 동적 검색 메서드에 **"조건 0개일 때 결과 N건"** 회귀 테스트가 있는가 (전체 조회 사고 방지)
- [ ] 사용자 입력으로 받은 정렬 컬럼을 **화이트리스트** 로 매핑하는가 (임의 컬럼 정렬 금지 — 인덱스 폭주)
- [ ] 페이지 크기에 상한이 있는가 (예: `size <= 100`)
- [ ] `keyword` 검색이 leading wildcard (`%키워드%`) 라면 인덱스를 못 탄다 — 정말 contains 인가, 아니면 prefix 인가?
- [ ] count 쿼리에 fetch join 이 들어있지 않은가 (Postgres 가 distinct + cartesian)
- [ ] Q클래스를 IDE 인덱싱 / VCS 제외 했는가 (`.gitignore` 의 `build/`)
- [ ] 로그에 검색 키워드가 PII 포함될 수 있는지 검토 (검색어 마스킹 정책)

## 7. 흔한 실수 & 디버깅

1. **Q클래스가 안 생겨 컴파일 실패**
   `annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")` 누락 / classifier `jakarta` 누락 / `jakarta.persistence-api` annotationProcessor 미등록 — 셋 중 하나. 빌드 후 `build/generated/sources/annotationProcessor/...` 확인.

2. **부팅 시 `JPAQueryFactory` 빈 없음**
   `QuerydslConfig` 에서 `@Bean` 으로 직접 만든다. Spring Data 가 자동으로 만들지 않음.

3. **`BooleanExpression` 가 `null` 반환 — 조건이 빠진 줄 모름**
   장점이자 함정. 조건 검증을 통합 테스트로 박아두자. (이 챕터의 `조건이_모두_null_이면_전체를_반환한다` 같은 테스트)

4. **`fetchCount()` 사용 — Deprecated**
   QueryDSL 5.x 부터 `Long count = query.select(post.count()).from(post).fetchOne()` 패턴 + `PageableExecutionUtils.getPage(...)` 권장.

5. **count 쿼리에 fetch join**
   `selectFrom(post).leftJoin(post.comments).fetchJoin()` 한 쿼리를 그대로 count 로 재사용하면 Postgres 가 cartesian product 카운트 → 숫자 부풀려짐. **content 쿼리와 count 쿼리는 분리**.

6. **정렬 컬럼을 사용자 입력 그대로 받아 `Sort.by(name)` → SQL Injection 은 아니지만 N+1 / 인덱스 미스**
   화이트리스트 switch 로 명시적 매핑. 본 챕터 `applyPaging()` 참고.

7. **Boot 4 패키지 이동 — `HibernatePropertiesCustomizer`**
   `spring-boot-autoconfigure.orm.jpa` → `spring-boot-hibernate.autoconfigure` 이동. ch08 에서 본 함정과 동일.

## 8. 더 깊이 — ch09b-jOOQ 와 비교 포인트

ch09 (QueryDSL) 의 명시적 한계 → ch09b 에서 다룰 영역:

| 카테고리 | QueryDSL | jOOQ |
|---|---|---|
| **방향성** | 클래스 → DB (ORM-First, JPA 기반) | DB → 클래스 (SQL-First, 스키마 우선) |
| **윈도우 함수** | 표준 미지원, native query 우회 | `ROW_NUMBER().over(partitionBy(...).orderBy(...))` 일급 |
| **CTE / WITH RECURSIVE** | 미지원 | `WITH RECURSIVE`, `LATERAL JOIN` 완전 지원 |
| **벤더 특화 SQL** | 우회 (`stringTemplate`, native) | PostgreSQL `jsonb`, `FOR UPDATE SKIP LOCKED`, `ON CONFLICT DO UPDATE` 네이티브 |
| **벌크 INSERT/UPDATE** | JPA 패러다임 (영속성 컨텍스트 경유) | 진짜 batch — SQL 한 방 |
| **타입 세이프 검증 시점** | 컴파일 (Q클래스) | 컴파일 (jOOQ 생성 클래스) — 둘 다 강력 |
| **트랜잭션** | JPA `@Transactional` | Spring TX 또는 jOOQ 자체 트랜잭션 API |
| **러닝 커브** | JPA 알면 자연스러움 | SQL 잘 알면 자연스러움 |
| **마이그레이션 통합** | Flyway/Liquibase 별도 | Flyway 와 같이 빌드 파이프라인 자연 결합 |

**선택 가이드** (ch09b 에서 자세히):
- **JPA/QueryDSL** 을 골라라: 도메인 중심 / 트랜잭션 스크립트 / 변경 패턴이 단순한 CRUD 위주
- **jOOQ** 를 골라라: 집계 리포트 / 통계 / 데이터 마이그레이션 / SQL 의 표현력 다 쓰고 싶을 때
- **두 개 같이** 쓸 수도 있다 — 트랜잭션 경계 안에서 JPA, 복잡 조회는 jOOQ.

### 본 챕터 코드의 jOOQ 비교 단서
- `PostQueryRepositoryImpl` 의 클래스 주석 끝에 "ch09b 에서 다룰 영역" 명시
- 프론트엔드 하단 카드에 동일 표

### 공식 문서
- [QueryDSL Reference](http://querydsl.com/static/querydsl/latest/reference/html_single/)
- [Spring Data JPA — Custom Repository Implementations](https://docs.spring.io/spring-data/jpa/reference/repositories/custom-implementations.html)
- [jOOQ Manual](https://www.jooq.org/doc/latest/manual/) (ch09b 진입 시)
