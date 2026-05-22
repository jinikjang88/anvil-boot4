# ch06-validation — 검증과 예외: Bean Validation + Problem Details (RFC 7807)

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.

## 1. 실생활 비유 (Why)

키오스크. 손님이 "음료 -3 개" 같은 헛소리를 *키 누르는 순간* 차단되고, "주문 수량 1~99" 같은 형식 오류는 그 자리에서 빨간 글씨로 알려준다. *통과한* 주문만 매장 안쪽으로 전달되어, 거기서는 "재고 부족" 같은 비즈니스 규칙만 신경 쓰면 된다.

서버 코드도 똑같다. *입구(컨트롤러)* 에서 형식/구문을 확실히 걸러내야 *안쪽(서비스/도메인)* 이 깨끗하게 일한다. 그리고 거절 응답은 **클라이언트가 자동으로 처리할 수 있는 형태** 여야 한다 — 그냥 400 한 줄로는 사용자 UX 가 안 만들어진다.

## 2. 진짜 현장 이야기 (War Story)

송금 서비스 초기 코드에서 검증을 컨트롤러에 일일이 `if (amount < 0) return ...` 식으로 박았다. 어느 날 한 엔드포인트에서 `if` 를 빠뜨려 음수 송금 요청이 그대로 도메인까지 들어갔고, 다행히 도메인 계층의 잔액 검증에 막혔지만 *그 검증마저 빠졌다면* 정산이 뒤집어졌을 사건. 코드리뷰에서 "왜 이건 검증하고 저건 안 하지?" 같은 질문이 매번 반복되어 PR 속도도 느렸다.

해결: **검증은 DTO 의 어노테이션으로 *선언적* 으로 박는다**. `@DecimalMin("100")` 한 줄이 "어떤 컨트롤러에서 받든 무조건 100 이상" 을 보장한다. 컨트롤러에는 `@Valid` 하나만. *if 빠뜨림* 이라는 카테고리의 버그가 통째로 사라진다.

여기에 더해 **응답 포맷도 표준화** (RFC 7807) — 클라이언트가 어디서 잘못됐는지 *파싱해서* UI 에 표시할 수 있다.

## 3. 핵심 개념 (What)

### 두 단계로 분리되는 검증

| 단계 | 무엇 | 어디 | 본 챕터의 사용처 |
|---|---|---|---|
| **1) 형식 / 구문** | 필수값, 길이, 범위, 패턴, 이메일 형식 | DTO + Bean Validation 어노테이션 + 컨트롤러 `@Valid` | `TransferRequest` |
| **2) 비즈니스 규칙** | "잔액 ≥ 금액", "한도 미초과" 등 도메인 룰 | 서비스 / 도메인 계층 | ch10 (트랜잭션) 에서 본격 |

본 챕터는 (1) 에 집중. (2) 는 ch10.

### 단일 필드 vs Cross-field

| 종류 | 예 | 표현 |
|---|---|---|
| 단일 필드 | `amount >= 100` | `@DecimalMin("100")` 표준 어노테이션 |
| Cross-field | `urgent=true` 면 `notifyEmail` 필수 | 직접 만든 `@UrgentRequiresNotifyEmail` (class-level) + `ConstraintValidator` |

### RFC 7807 Problem Details

응답 본문 표준:
```json
{
  "type":     "https://anvil.run/errors/validation",
  "title":    "Validation Failed",
  "status":   400,
  "detail":   "요청 본문이 검증 규칙을 위반했습니다",
  "instance": "/api/v1/transfers",
  "errors": [
    { "field":"amount", "rejectedValue":-100, "code":"DecimalMin",
      "message":"amount 는 100 이상이어야 합니다" }
  ]
}
```

`type`/`title`/`status`/`detail`/`instance` 는 표준 필드. `errors[]` 는 *확장 속성* — RFC 7807 이 권장하는 패턴(필요한 정보를 자유롭게 덧붙이라).

Content-Type 은 `application/problem+json`.

## 4. 코드로 벼리기 (How)

```bash
./gradlew :chapters:ch06-validation:bootRun

# 1) 정상 송금 → 201 + Location
curl -i -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{"fromAccount":"ACC-1","toAccount":"ACC-2","amount":10000,"currency":"KRW",
       "options":{"urgent":false}}'

# 2) 음수 금액 → 400 ProblemDetail
curl -i -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{"fromAccount":"A","toAccount":"B","amount":-100,"currency":"KRW",
       "options":{"urgent":false}}'

# 3) urgent + email 없음 → cross-field validator 가 잡음
curl -i -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{"fromAccount":"A","toAccount":"B","amount":5000,"currency":"KRW",
       "options":{"urgent":true}}'

# 4) 여러 위반 → errors[] 에 모두 담김
curl -s -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{"fromAccount":"","toAccount":"","amount":-1,"currency":"won","options":{"urgent":true}}' \
  | python3 -m json.tool

# 테스트
./gradlew :chapters:ch06-validation:test
```

### 핵심 파일

| 파일 | 역할 |
|---|---|
| `domain/TransferRequest.java` | record + 표준 어노테이션 + class-level cross-field |
| `domain/TransferOptions.java` | 중첩 record — 부모의 `@Valid` 가 재귀 검증 |
| `validation/UrgentRequiresNotifyEmail.java` | custom annotation |
| `validation/UrgentNotifyEmailValidator.java` | ConstraintValidator + field path 매핑 |
| `controller/TransferController.java` | `@Valid @RequestBody` 한 줄 |
| `controller/ValidationExceptionHandler.java` | `@RestControllerAdvice` + `ProblemDetail` |

### ch05 → ch06 의 흐름

ch05 의 임시 `RestExceptionHandler` 는 `Map.of("error", ex.getMessage())` 한 줄 응답이었다. ch06 에서 **그 자리를 RFC 7807 로 업그레이드** 하는 셈. 같은 도메인 예외도 클라이언트가 *프로그래밍 가능* 한 형태로 받게 된다.

## 5. 시각화 (See)

`frontend/index.html` — 송금 폼 + 빠른 시나리오 버튼 + ProblemDetail 응답 표시.

```bash
./gradlew :chapters:ch06-validation:bootRun                                # 8080
python3 -m http.server 5173 --directory chapters/ch06-validation/frontend  # 5173
# → http://localhost:5173
```

핵심 시연:
- 상단 시나리오 버튼 (6개) 으로 폼이 자동 채워짐
- "송금 요청" 클릭 → 응답이 정상이면 우측 컬렉션에 추가, 실패면 `errors[]` 의 각 `field` 를 따라가 **해당 입력칸 아래에 빨간 메시지 + 입력칸 빨간 테두리**
- 응답 카드에는 ProblemDetail JSON 원본 표시

이게 RFC 7807 의 진짜 효용 — 클라이언트가 *어디가 잘못됐는지* 응답만 보고 UI 에 그릴 수 있다.

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)

- [ ] **검증은 DTO 에 선언적으로** — `if (x == null)` 식 수동 검증 보이면 PR 막기
- [ ] **`@Valid` 누락이 가장 흔한 버그** — `@RequestBody` 옆에 항상 같이 다니는지 확인
- [ ] **에러 응답에 PII echo 주의** — `rejectedValue` 에 주민번호/카드번호 같은 값이 그대로 들어가면 위험. 민감 필드는 응답에서 마스킹 (`SecretMasker` 패턴 — ch04)
- [ ] **에러 메시지 다국어화** — `message` 를 `messages.properties` 로 분리 (`@DecimalMin(message = "{transfer.amount.min}")`). 본 챕터는 단순화로 한글 직접 박음
- [ ] **에러 응답의 안정된 인터페이스** — `code` 필드(예: `DecimalMin`, `UrgentRequiresNotifyEmail`) 가 *클라이언트의 분기 키*. 함부로 바꾸지 말 것
- [ ] **`@Validated` vs `@Valid` 차이 인식** — `@RequestParam`/`@PathVariable` 검증엔 컨트롤러 클래스에 `@Validated` 가 필요. 본 챕터는 body 만 다룸
- [ ] **5xx vs 4xx 시맨틱** — 서버 버그(예: NPE)는 5xx, 클라이언트 잘못은 4xx. 검증 실패는 *항상* 400/422

## 7. 흔한 실수 & 디버깅

1. **`@Valid` 누락 → 검증이 안 돎**
   `@RequestBody` 옆에 `@Valid` 가 빠지면 어노테이션이 그냥 무시된다. 부팅 시 에러도 없음. 테스트로 잡거나 PR 체크리스트로.

2. **중첩 record 검증 누락**
   부모 record 필드에도 `@Valid` 가 있어야 자식 record 의 어노테이션이 동작한다. 본 챕터의 `@Valid TransferOptions options` 가 그 예.

3. **`errors[]` 의 field 가 `options.notifyEmail` 이 안 되고 클래스 루트로 잡힘**
   cross-field validator 에서 `disableDefaultConstraintViolation()` + `addPropertyNode("options").addPropertyNode("notifyEmail")` 로 명시. 안 그러면 클라이언트가 어디에 에러를 표시할지 못 정함.

4. **`@NotNull` vs `@NotBlank` vs `@NotEmpty`**
   - `@NotNull` — 객체 null 차단 (빈 문자열 통과)
   - `@NotBlank` — 문자열 + null/공백 차단
   - `@NotEmpty` — 컬렉션 + null/빈 차단
   String 필드엔 보통 `@NotBlank`.

5. **`@ControllerAdvice` 가 매핑한 예외 우선순위**
   여러 핸들러가 매칭되면 가장 *구체적인* 타입이 이긴다. 일반 `Exception.class` 핸들러는 fallback 으로만.

6. **응답에 `Content-Type: application/problem+json` 누락**
   클라이언트가 "ProblemDetail 이구나" 인식하려면 이 Content-Type 이 필수. `ResponseEntity.contentType(MediaType.APPLICATION_PROBLEM_JSON)` 명시.

## 8. 더 깊이 (선택)

- RFC 7807 — Problem Details for HTTP APIs (원문)
- RFC 9457 — Problem Details for HTTP APIs (개정판, 2023)
- Jakarta Validation 3.x 스펙
- Spring Boot 의 `spring.mvc.problemdetails.enabled` 옵션 — 내장 핸들러를 ProblemDetail 로 통일
- Hibernate Validator — Jakarta Validation 의 참조 구현
- `@Validated` + group — 같은 DTO 를 시나리오별로 다른 규칙으로 검증
