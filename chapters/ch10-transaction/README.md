# ch10-transaction — 트랜잭션 — 격리·전파·AOP 함정

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.

## 1. 실생활 비유 (Why)

트랜잭션은 **은행 창구의 유리칸막이**다.
- 내가 송금하는 동안 다른 사람이 내 통장을 볼 수 없어야 하고 (격리),
- 중간에 실패하면 보낸 돈도 받은 돈도 없던 일이 되어야 한다 (원자성).
- 칸막이가 얇으면 옆 사람이 내 거래를 볼 수 있고 (격리 수준),
- 칸막이를 안 치면 양쪽이 동시에 한 통장에 손을 대서 돈이 증발한다 (Lost Update).

## 2. 진짜 현장 이야기 (War Story)

포인트 차감 API — `@Transactional` 한 줄이면 되는데:
1. **checked exception 함정**: 외부 PG 연동이 `throws Exception` (checked) → 차감 커밋, 결제 실패 → 포인트만 증발. `rollbackFor = Exception.class` 한 줄로 수정.
2. **self-invocation 함정**: 같은 클래스 내에서 `@Transactional` 메서드를 호출 → AOP 프록시 안 탐 → 트랜잭션 없이 실행 → 부분 커밋 후 장애. 별도 빈으로 분리해서 해결.
3. **데드락**: 두 사용자가 동시에 상대방에게 포인트 전송 → DB가 deadlock detected → retry 없이 500 응답 → CS 폭주. 순서 정렬(항상 id 작은 쪽 먼저 락) 로 해결.

## 3. 핵심 개념 (What)

| 비유 | 개념 | 구현 |
|---|---|---|
| 칸막이 두께 | **격리 수준** | `@Transactional(isolation = ...)` |
| 칸막이 중첩 | **전파** | `Propagation.REQUIRED / REQUIRES_NEW` |
| 유리 없는 칸막이 | AOP 프록시 우회 | self-invocation / private method |
| 양쪽이 서로 기다림 | **데드락** | 비관적 락 순서 역전 |
| 사인 실패 시 원복 | **롤백** | RuntimeException 만 기본 — checked 는? |

### 격리 수준 (PostgreSQL 기준)
| 수준 | Dirty Read | Non-Repeatable Read | Phantom | 성능 |
|---|---|---|---|---|
| READ COMMITTED (기본) | X | O | O | 높음 |
| REPEATABLE READ | X | X | X(PG) | 중간 |
| SERIALIZABLE | X | X | X | 낮음 |

### 전파 (Propagation)
| 전파 | 바깥 TX 있을 때 | 바깥 TX 없을 때 | 학습 포인트 |
|---|---|---|---|
| REQUIRED (기본) | 합류 | 새로 만듦 | 바깥 롤백 시 같이 롤백 |
| REQUIRES_NEW | 독립 TX | 새로 만듦 | 바깥 롤백과 무관 — 감사 로그에 사용 |

## 4. 코드로 벼리기 (How)

```bash
# 1) Postgres
docker compose -f docker/docker-compose.local.yml up -d postgres

# 2) 백엔드 실행 (3 계좌 시드: 민지/준호/지훈 각 10만원)
./gradlew :chapters:ch10-transaction:bootRun

# 3) 송금
curl -s -XPOST localhost:8080/api/v1/tx/isolation/read-committed \
  -H 'content-type: application/json' \
  -d '{"fromId":1,"toId":2,"amount":30000}' | jq '{success, total: .totalBalance}'

# 4) 리셋
curl -s -XPOST localhost:8080/api/v1/tx/reset | jq '{success}'
```

### 핵심 코드
| 파일 | 역할 |
|---|---|
| `domain/Account.java` | `@Version` (낙관적 락) + `withdraw/deposit` |
| `domain/TxLog.java` | 전파 시연용 이벤트 로그 |
| `service/TransferService.java` | 4축 메인 — 격리별 송금, 전파 시연, AOP 함정, 데드락 |
| `service/TxLogService.java` | **별도 빈** — REQUIRED vs REQUIRES_NEW 전파 분리 |
| `repository/AccountRepository.java` | `findByIdForUpdate` (비관적 락), `sumBalance` (conservation) |

## 5. 시각화 (See)

`frontend/index.html` — 4축 버튼 그리드 + 계좌 현황 + conservation 배지.

```bash
python3 -m http.server 5173 --directory chapters/ch10-transaction/frontend
```

체험 시나리오:
1. "리셋" → 3계좌 각 10만원. 합계 30만원 (초록).
2. READ_COMMITTED 송금 → 합계 30만원 유지 (정상).
3. "checked exception" → 합계 **29만원** (빨강!) → 출금만 커밋됨.
4. "리셋" → "checked-exception-fixed" → 합계 30만원 유지 (rollbackFor 덕).
5. "REQUIRED + 롤백" → TxLog 없음 (바깥과 같이 롤백됨).
6. "REQUIRES_NEW + 롤백" → TxLog 1건 (독립 TX 라 살아남음).
7. "데드락" → FAIL + deadlock 메시지.

## 6. 실무 체크리스트 (ISMS-P / 성능)
- [ ] `@Transactional` 의 `rollbackFor` 가 `Exception.class` 를 포함하는가
- [ ] self-invocation 이 없는가 (같은 클래스 내 `@Transactional` 호출은 프록시 우회)
- [ ] 긴 트랜잭션은 없는가 (외부 API 호출을 TX 밖에서)
- [ ] 비관적 락 사용 시 **lock 순서** 가 일관된가 (항상 id ASC — 데드락 방지)
- [ ] `@Transactional(readOnly = true)` 를 읽기 전용에 쓰고 있는가
- [ ] `@Version` (낙관적 락) 재시도 정책이 있는가
- [ ] 트랜잭션 로그 (`org.springframework.transaction: DEBUG`) 를 개발 환경에서 켜고 있는가

## 7. 흔한 실수 & 디버깅

1. **self-invocation — 트랜잭션이 안 걸리는 미스터리**
   `this.internalMethod()` 는 프록시가 아닌 실제 객체의 메서드 호출. 해법: 별도 빈 분리.

2. **checked exception → 커밋**
   `@Transactional` 기본은 `RuntimeException` + `Error` 만 롤백. 해법: `rollbackFor = Exception.class`.

3. **private @Transactional 메서드 — 무시됨**
   Spring AOP 는 public 메서드만 가로챈다.

4. **긴 트랜잭션 — DB 커넥션 고갈**
   외부 API 호출을 `@Transactional` 안에서 하면 응답 대기 동안 커넥션 점유. TX 밖에서 호출.

5. **데드락 — 무조건 retry**
   해법: lock 순서 통일 (항상 id 작은 쪽 먼저) 또는 `@Retryable`.

6. **낙관적 락 OptimisticLockException**
   `@Version` 값이 읽은 시점과 다르면 실패. 사용자에게 "다시 시도" 유도.

7. **REQUIRES_NEW 안에서 바깥 TX entity 접근 → LazyInitializationException**
   REQUIRES_NEW 는 새 EntityManager. 바깥의 영속성 컨텍스트 일시 중단.

## 8. 더 깊이 (선택)
- `TransactionTemplate` — 프로그래밍 방식 트랜잭션
- `@TransactionalEventListener(phase = AFTER_COMMIT)` — 커밋 후 이벤트
- 분산 트랜잭션 / Saga 패턴 — MSA 에서 `@Transactional` 은 단일 DB 한정
- 공식 문서: [Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
