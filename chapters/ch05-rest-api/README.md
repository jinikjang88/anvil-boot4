# ch05-rest-api — REST API 설계, 메뉴판처럼 만들기

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.

## 1. 실생활 비유 (Why)

음식점 메뉴판이 잘 짜여 있으면 손님이 헷갈리지 않는다. *URL 이 메뉴판, HTTP 메서드가 주문 행위*.

- **메뉴판(URL)** 은 명사로만 적는다. "치킨" 이라고 쓰지 "치킨_주문하기" 라고 안 쓴다.
- **주문 행위(HTTP 메서드)** 가 동사를 담당한다 — POST(시킴) / GET(메뉴 봄) / PUT(주문 변경) / DELETE(취소)
- 잘못된 주문엔 **명확한 신호(상태코드)** — "재료 떨어짐(404)" vs "재고 있는데 신용카드 한도 초과(409)" 는 다른 답이어야 한다

이 챕터는 주문(Order) 리소스 하나를 *REST 정석* 대로 깎는다.

## 2. 진짜 현장 이야기 (War Story)

베민 / 금융 메시징을 거치며 본 어색한 URL 들:

- `POST /api/v1/order/list/get` — POST 로 목록을 GET 함 (메서드와 URL 둘 다 동사)
- `GET  /api/v1/getUserById?id=42` — GET 인데 동사가 또 들어감
- `POST /api/v1/order/cancel/{id}` — DELETE 가 있는데 "취소 전용" 동사 URL 을 만듦
- 모두 200 — *실패도 200 응답에 `{"success":false}`*. 모니터링 시스템에서 에러율 잡히지 않음

문제는 단순한 일관성이 아니다 — **상태코드를 안 쓰면 클라이언트와 인프라가 "에러" 를 인지하지 못한다.** API Gateway 의 retry 정책, 모니터링 알람, SDK 의 자동 에러 핸들링 — 전부 HTTP 상태코드를 본다. 200 으로 도배된 API 는 *전체 인프라 생태계와 단절된* 셈.

교훈: REST 컨벤션은 미적인 문제가 아니라 **운영 자동화의 인터페이스**.

## 3. 핵심 개념 (What)

### Richardson Maturity Model

| 레벨 | 무엇 | 본 챕터의 상태 |
|---|---|---|
| 0 | HTTP over RPC — POST 한 엔드포인트로 모든 걸 처리 | 안티패턴 |
| 1 | 리소스 분리 — URL 이 명사 | ✓ `/api/v1/orders` |
| 2 | HTTP 메서드 + 상태코드 활용 | ✓ POST/GET/PATCH/PUT/DELETE + 201/200/204/400/404/409 |
| 3 | HATEOAS — 응답에 다음 액션 링크 포함 | 본 챕터 범위 외 |

본 챕터는 Level 2 가 목표. 대부분의 *현실적인 REST API* 가 여기서 멈춘다 (Level 3 은 클라이언트가 받쳐줘야 의미가 있어 보통 과투자).

### HTTP 메서드의 시맨틱

| 메서드 | 의미 | 멱등? | 안전? |
|---|---|---|---|
| GET    | 조회 | ✓ | ✓ |
| POST   | 생성 / 비멱등 액션 | ✗ | ✗ |
| PUT    | 전체 교체 | ✓ | ✗ |
| PATCH  | 부분 수정 | △ (구현에 따라) | ✗ |
| DELETE | 삭제 | ✓ (이미 없는 것 또 지워도 같은 결과) | ✗ |

**멱등** 이 왜 중요한가 — 클라이언트의 자동 재시도, 게이트웨이의 retry 정책이 안전하게 동작하려면 GET/PUT/DELETE 가 멱등해야 한다.

### URL 설계 5 규칙

1. **명사로** — `/orders` (O), `/getOrders` (X)
2. **복수형** — `/orders` (O), `/order` (X)
3. **계층** — `/orders/{id}/items/{itemId}` 식으로 소유 관계 표현
4. **kebab-case** — `/order-items` (O), `/orderItems` (X)
5. **버전 prefix** — `/api/v1/orders`

## 4. 코드로 벼리기 (How)

```bash
./gradlew :chapters:ch05-rest-api:bootRun

# 1) POST — 생성
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"customer":"alice","items":[{"sku":"sku-1","name":"도토리","quantity":2,"price":1500}],"memo":"빠르게"}'
# → HTTP/1.1 201
#   Location: /api/v1/orders/{uuid}

# 2) GET — 목록 (필터 + 페이지)
curl 'http://localhost:8080/api/v1/orders?status=PENDING&page=0&size=10'

# 3) GET — 단건
curl http://localhost:8080/api/v1/orders/{id}

# 4) PATCH — 부분 수정 (memo)
curl -X PATCH http://localhost:8080/api/v1/orders/{id} \
  -H 'Content-Type: application/json' \
  -d '{"memo":"변경"}'

# 5) PUT — 상태 전이 (단일 필드 교체)
curl -X PUT http://localhost:8080/api/v1/orders/{id}/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"CONFIRMED"}'

# 6) 잘못된 전이 → 409
curl -i -X PUT http://localhost:8080/api/v1/orders/{id}/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"DELIVERED"}'
# → HTTP/1.1 409 Conflict

# 7) DELETE — 204
curl -i -X DELETE http://localhost:8080/api/v1/orders/{id}

# 테스트
./gradlew :chapters:ch05-rest-api:test
```

### 핵심 파일 매핑

| 파일 | 역할 |
|---|---|
| `domain/OrderStatus.java` | enum |
| `domain/OrderItem.java` (record) | 라인 아이템 + compact constructor 검증 |
| `domain/Order.java` (record) | 도메인 + 응답 DTO 겸용 |
| `domain/OrderStateMachine.java` | 전이 규칙 단일 출처 |
| `repository/OrderRepository.java` | in-memory ConcurrentHashMap |
| `service/OrderService.java` | CRUD + 전이 오케스트레이션 |
| `controller/OrderController.java` | REST 매핑 (5 메서드) |
| `controller/RestExceptionHandler.java` | 도메인 예외 → 상태코드 |

## 5. 시각화 (See)

`frontend/index.html` — 메뉴판 카드 6개(POST/GET목록/GET단건/PATCH/PUT/DELETE) + 오른쪽 컬렉션 표.
호출하면 응답 상태코드 + Location 헤더 + 본문이 카드 안에 표시되고, 컬렉션이 자동 갱신된다.
주문 status 는 오행 컬러로 구분 (PENDING 회색 / CONFIRMED 초록 / SHIPPED 파랑 / DELIVERED 진초록 / CANCELLED 빨강).

```bash
./gradlew :chapters:ch05-rest-api:bootRun                                # 8080
python3 -m http.server 5173 --directory chapters/ch05-rest-api/frontend  # 5173
```

해보면 좋은 시나리오:
1. POST 한 번 → id 자동 캡처되어 다른 카드들의 `{id}` 가 채워짐
2. PUT status `CONFIRMED` → 200, 다시 `SHIPPED` → 200, `DELIVERED` → 200
3. 위에서 다시 `PUT status PENDING` → **409 Conflict** (역행 차단)
4. DELETE → 204, 같은 id 다시 DELETE → **404**

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)

- [ ] **URL 에 동사 금지** — 코드리뷰에서 `/get`, `/list`, `/save` 같은 동사가 보이면 PR 막기
- [ ] **상태코드 일관성** — 같은 종류 에러는 항상 같은 코드. 400 vs 404 vs 409 vs 422 의 시맨틱 차이를 팀 위키에 박아두기
- [ ] **POST 의 자동 재시도 함정** — POST 는 비멱등. 클라이언트가 재시도하면 중복 생성. 해결책: `Idempotency-Key` 헤더 + 서버 측 dedupe (ch11 캐시 또는 DB 유니크 제약)
- [ ] **응답 본문에 PII 노출 점검** — `customer` 같은 필드가 PII 일 수 있음. 응답 마스킹 (ch04 SecretMasker 와 동일 패턴)
- [ ] **버전 호환성 정책** — v1 deprecation 전에 v2 출시 + 충분한 공지 기간. 절대 v1 의 시맨틱을 *조용히* 바꾸지 않기
- [ ] **rate limit / size limit** — 페이지 size 상한 (본 챕터: 100), 본문 크기 제한, 인증된 사용자별 한도 (ch04 RateLimiter 와 ch07 인증 결합)
- [ ] **Allow 헤더 / OPTIONS** — 잘못된 메서드 호출엔 405 와 `Allow: GET, POST` 등의 안내 (Spring Boot 기본 동작 확인)

## 7. 흔한 실수 & 디버깅

1. **모든 응답이 200 — 에러도 `{"success":false}` 로**
   클라이언트 자동 retry / 모니터링이 망가짐. 항상 적절한 4xx/5xx 사용.

2. **POST 로 단순 조회 — "body 가 길어서" 라는 이유**
   GET 도 body 보낼 수 있고 (RFC 9110), 정 안 되면 검색 전용 엔드포인트 `POST /orders/search` 로 분리. 평범한 조회를 POST 로 둘 핑계는 없음.

3. **PUT 으로 부분 수정**
   PUT 시맨틱은 전체 교체. 클라이언트가 누락한 필드가 `null` 로 들어가 데이터 손실. 부분 수정은 PATCH 가 정답.

4. **PATCH 의 body 포맷이 매번 다름**
   본 챕터처럼 도메인-specific PATCH (memo 만 수정) 가 단순하고 명확. JSON Patch (RFC 6902) / JSON Merge Patch (RFC 7396) 는 운영 도구라면 모를까 일반 비즈니스 API 엔 과투자.

5. **삭제했는데 200 + body 반환**
   DELETE 성공은 보통 **204 No Content**. 굳이 본문 보내야 한다면 200 + 명확한 이유 (예: 비동기 큐잉 결과). 본 챕터는 204.

6. **PUT/PATCH/DELETE 가 CORS 차단**
   `allowedMethods` 에 다 포함시켜야 함. 본 챕터 `LocalDevCorsConfig` 가 5종 모두 허용.

7. **`/api/v1` 부터 시작 안 하고 나중에 추가**
   초기엔 v 없이 시작 → 호환 깨질 때 *모든 클라이언트에 v 추가 요청* 하느라 6 개월. 처음부터 `/v1` prefix.

## 8. 더 깊이 (선택)

- Richardson Maturity Model — Martin Fowler 의 원글
- RFC 9110 — HTTP Semantics (메서드 / 상태코드의 표준 정의)
- RFC 7807 — Problem Details for HTTP APIs (ch06 에서 적용)
- API Design Guide — Microsoft / Google / Zalando 등 회사별 가이드 비교
- URL 버저닝 vs 헤더 버저닝 vs 미디어타입 버저닝 — 트레이드오프
- HATEOAS — Spring HATEOAS 라이브러리 (Level 3, 신중히 도입)
