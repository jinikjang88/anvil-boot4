# ch02-java25-features — Java 25 신문법 한 입씩

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.

## 1. 실생활 비유 (Why)

알바생을 잔뜩 고용하는 일과 같다.

- **플랫폼 스레드** = 정규직. OS 자원이 비싸서 10명 이상 못 고용한다. 한 사람이 손님 한 명 응대 중에 주방 음식 기다리느라(=I/O) 멍 때려도 다른 손님은 못 받는다.
- **Virtual Thread** = 시급 알바. 한 명 고용하는 비용이 거의 공짜라 N명을 그냥 부른다. 알바가 멍 때리는 동안엔 자리(=캐리어 스레드)를 다른 알바한테 양보한다.

I/O 가 많은 가게(=외부 API 호출이 많은 백엔드)일수록 알바 모델이 압도적으로 유리하다. Java 25 는 이 알바 고용을 **언어 차원의 표준 기본기**로 끌어올렸다.

여기에 더해 신문법 셋이 같이 일한다:
- **Record** — 60 줄짜리 DTO 가 한 줄로
- **Sealed Interface** — "결과 타입은 이 셋뿐" 컴파일러에 못박기
- **Pattern Matching for switch** — sealed + record 를 우아하게 분기

## 2. 진짜 현장 이야기 (War Story)

결제 알림을 외부 PG / 은행망 / 알림 채널 3 곳에 동시 통보해야 하는 시스템이 있었다. 처음 구현은 `RestTemplate` 동기 호출 셋을 줄세웠다. 각 200~400ms × 3 = 약 1초. TPS 200 이면 worker pool(20) 이 일주일에 한 번씩 포화 → 큐 적체 → 알림 지연.

`Executors.newVirtualThreadPerTaskExecutor()` 로 fan-out 만 바꿨다. 코드 변경은 거의 없었고 (executor 만 교체), 응답시간은 1000ms → 250ms, 풀 포화는 사라지고 동일 인스턴스가 **TPS 1000+** 을 받아냈다. *"코드 한 줄로 5 배"* 가 가장 가까운 표현.

핵심은 "외부 호출이 sleep 하는 동안에는 자리를 양보한다" 라는 단순한 원칙이 *언어 차원에서* 동작한다는 것. 직접 비동기/리액티브로 갈아엎을 필요가 없어졌다.

## 3. 핵심 개념 (What)

| 비유 | Java 25 개념 | 본 챕터의 사용처 |
|---|---|---|
| 시급 알바를 N명 고용 | **Virtual Thread** (JEP 444, 표준) | `OrderProcessor#process` 가 받는 executor 의 종류 |
| 정해진 양식 카드 | **Record** | `OrderRequest`, `BatchSummary`, `BatchRequest` |
| "결과 종류는 이 셋뿐" | **Sealed Interface** | `OrderResult` permits `Approved / Rejected / Pending` |
| 카드 종류별로 분기 | **Pattern Matching for switch** (+ record deconstruction) | `OrderProcessor#describe` / `countByType` |

도메인은 의도적으로 단순하다. **같은 도메인 로직** 을 두 executor 로 돌렸을 때 응답시간 차이가 *오롯이* executor 의 동시성 모델 차이라는 걸 눈으로 확인하기 위함.

## 4. 코드로 벼리기 (How)

```bash
# 백엔드
./gradlew :chapters:ch02-java25-features:bootRun

# 호출 (count 만 필수, amount 범위는 생략 시 1000~50000)
curl -X POST http://localhost:8080/api/orders/process-platform \
  -H 'Content-Type: application/json' -d '{"count": 100}'

curl -X POST http://localhost:8080/api/orders/process-virtual \
  -H 'Content-Type: application/json' -d '{"count": 100}'

# 응답 예
# { "executor":"virtual", "total":100, "elapsedMs":237,
#   "counts":{"approved":99,"rejected":0,"pending":1},
#   "samples":["OK  order-0000 -> tx-order-0000", ...] }
```

```bash
# 테스트
./gradlew :chapters:ch02-java25-features:test
```

핵심 파일:

| 파일 | 역할 |
|---|---|
| `domain/OrderRequest.java` | record + compact constructor 검증 |
| `domain/OrderResult.java` | sealed interface + 3 record (Approved/Rejected/Pending) |
| `domain/PaymentGateway.java` | 200ms 외부 호출 시뮬레이션 |
| `service/OrderProcessor.java` | executor 주입 + pattern matching switch + record deconstruction |
| `controller/OrderController.java` | platform / virtual 두 엔드포인트 |

## 5. 시각화 (See)

`frontend/index.html` — Chart.js 막대 차트로 두 executor 의 elapsed 시간을 비교한다.
오행 컬러: **火**(Platform) vs **木**(Virtual).

```bash
./gradlew :chapters:ch02-java25-features:bootRun                                       # 8080
python3 -m http.server 5173 --directory chapters/ch02-java25-features/frontend         # 5173
# → http://localhost:5173
```

슬라이더로 동시 요청 수를 10~500 사이에서 조절. count 가 풀 크기(10) 의 배수일수록 차이가 극적으로 보인다 (100 건이면 platform 약 2 초 / virtual 약 0.25 초 = 8배 안팎).

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)

- [ ] **synchronized 블록 vs ReentrantLock** — Virtual Thread 가 `synchronized` 안에서는 캐리어 스레드를 못 놓는다 (JEP 491 로 개선 진행). 핫패스에 큰 `synchronized` 가 있으면 ReentrantLock 으로 바꿀지 검토
- [ ] **JDBC 호출** — 드라이버가 동기 블로킹이라도 Virtual Thread 가 alleviate. 다만 커넥션 풀 크기가 새로운 병목이 됨 → HikariCP 사이즈를 *DB 쪽 한도* 기준으로 다시 산정
- [ ] **ThreadLocal 사용** — Virtual Thread 는 N 만 단위로 생성 가능. ThreadLocal 에 큰 객체를 저장하면 메모리 곱하기 N — 가능한 ScopedValue 로 마이그레이션
- [ ] **외부 API 호출 동시성 상한** — virtual 이라고 무한정 띄우면 외부가 throttle/장애. Semaphore 로 명시적 동시성 상한 설정
- [ ] **개인정보 로깅** — `samples` 같은 디버깅 응답에 PII 가 섞이지 않도록 도메인 record 의 `toString` 을 의도적으로 가리기 (ch07 에서 본격 처리)

## 7. 흔한 실수 & 디버깅

1. **`Executors.newVirtualThreadPerTaskExecutor()` 의 close 누락**
   try-with-resources 로 감싸지 않으면 컨트롤러 호출마다 executor 가 누적된다. ExecutorService 는 AutoCloseable — `try (ExecutorService e = ...)` 패턴 사용.

2. **벤치 측정에 warmup 무시**
   JIT 가 핫스팟을 컴파일할 시간이 필요하다. 본 데모처럼 200ms × N 대용량 I/O 라면 warmup 없이도 차이가 크게 보이지만, CPU 바운드 비교라면 JMH 같은 정식 벤치를 써야 한다.

3. **sealed 의 nested record 가 외부에서도 보임**
   `OrderResult.Approved` 처럼 `Outer.Inner` 형태로 외부에서 접근 가능하다 — 의도적으로 캡슐화하려면 `non-sealed` / `final` 키워드를 명시적으로 통제.

4. **`switch` 안에서 `null` 만 빼먹기**
   Java 21+ 패턴 매칭은 `case null` 을 명시할 수 있다. 본 챕터에선 sealed 가 not-null 만 다루지만, 일반 객체 패턴 매칭 땐 `case null -> ...` 분기를 잊지 말 것.

## 8. 더 깊이 (선택)

- JEP 444 — Virtual Threads
- JEP 440 / 441 — Record Patterns, Pattern Matching for switch
- JEP 409 — Sealed Classes
- Brian Goetz, "Project Loom — Why Virtual Threads Need You"
- HikariCP 커넥션 풀 사이징 가이드 (Virtual Thread 시대의 재정렬)
