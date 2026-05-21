# ch04-config — 설정의 기술: local / dev / prod 와 시크릿 분리

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.

## 1. 실생활 비유 (Why)

여행 갈 때 짐 싸기와 같다.

- **국내여행(local)** — 가벼운 백팩, 충전기, 빈 통장. 잊고 가도 집에서 가져오면 됨
- **해외여행(dev)** — 캐리어, 어댑터, 일부 현금. 잊으면 살 수도 있지만 비쌈
- **장기 출장(prod)** — 풀 패킹, 노트북, 진짜 신분증과 카드. 잊으면 안 됨

**여권(시크릿)** 은 가방에 그냥 넣지 않는다 — 별도 보관함에, 사진은 클라우드 백업,
원본은 호텔 금고. 코드 저장소에 두는 건 **여권을 길거리에 떨어뜨리는 짓**.

이 챕터는 *같은 코드* 가 환경마다 다르게 동작하도록 만드는 법, 그리고 시크릿을 *어떻게* 분리하는지 만져 본다.

## 2. 진짜 현장 이야기 (War Story)

신입 시절 어느 새벽 3시, 슬랙 알림. **"prod DB 비밀번호가 git 에 푸시됨."**
누군가 `application.yml` 에 임시 DB 정보 적어놓고 commit 한 뒤, "나중에 빼야지" 하다가
PR 머지. 다행히 private 레포였지만 사내 정책상 24h 안에 비밀번호 전체 회전(rotation)이 강제됐다.
DBA / 인프라 / 백엔드 5명이 새벽까지 모든 환경의 DB 패스워드를 돌리고, IAM 키 갱신하고,
관련 서비스 모두 재배포. 그날 이후 PR 체크리스트 1번이 *"시크릿 들어갔는지"* 가 됐다.

기술적 교훈 셋:
1. 시크릿 파일은 **별도 yml** + `.gitignore` 차단
2. `application-{profile}.yml` 만으로는 부족 — prod 시크릿은 **외부 주입(환경변수/Secrets Manager)**
3. 응답/로그에서도 마스킹 (방어선 두 겹)

## 3. 핵심 개념 (What)

| 비유 | Spring Boot 개념 | 본 챕터의 사용처 |
|---|---|---|
| 기본 짐 (공통) | `application.yml` | `anvil.*` 기본값 |
| 여행 종류별 추가 | `application-{profile}.yml` | local / dev / prod 별 오버라이드 |
| 여권 별도 보관 | `application-local-secret.yml` (gitignored) | sender.api-key |
| 호텔 금고 | 환경변수 / Secrets Manager | prod 의 시크릿 주입 경로 |
| 짐 목록 체크리스트 | `@ConfigurationProperties` record | `AppProperties` 타입 안전 바인딩 |
| 짐 어디서 왔는지 추적 | `Environment.getPropertySources()` | `ConfigInspector` |
| 가방 라벨링 (환경 표시) | `@Profile("local")` | `LocalDevCorsConfig` (이전 챕터에서 이미 사용) |

### PropertySource 우선순위 (높은 → 낮은)

```
1. CommandLine arguments       (--anvil.name=foo)
2. SPRING_APPLICATION_JSON     (환경변수로 JSON 주입)
3. ServletConfig / ServletContext
4. JNDI
5. System properties           (-D 옵션)
6. System environment          (ENV_VAR)
7. RandomValuePropertySource
8. application-{profile}.yml   (활성 프로파일별)
9. application.yml             (공통)
10. SpringApplication.setDefaultProperties()
```

본 챕터의 `/api/config/sources` 엔드포인트가 실제 순서를 그대로 보여준다.

## 4. 코드로 벼리기 (How)

```bash
# 기본(local) 으로 실행
./gradlew :chapters:ch04-config:bootRun

# profile 바꿔서 실행
SPRING_PROFILES_ACTIVE=dev  ./gradlew :chapters:ch04-config:bootRun
SPRING_PROFILES_ACTIVE=prod ./gradlew :chapters:ch04-config:bootRun

# 활성 설정 (시크릿 마스킹된 형태)
curl http://localhost:8080/api/config/active | python3 -m json.tool

# 특정 키가 어디서 왔는지 추적
curl "http://localhost:8080/api/config/source?key=anvil.name"
curl "http://localhost:8080/api/config/source?key=anvil.sender.api-key"

# 환경변수로 키 오버라이드 — 우선순위 시연
ANVIL_NAME=overridden \
  ./gradlew :chapters:ch04-config:bootRun
# → /api/config/source?key=anvil.name 의 source 가 systemEnvironment 로 바뀜

# 알림 발송 (rate limit 적용)
curl -X POST http://localhost:8080/api/notify \
  -H 'Content-Type: application/json' \
  -d '{"recipient":"u@x.com","message":"hi"}'

# 테스트
./gradlew :chapters:ch04-config:test
```

### 시크릿 파일 사용법

```bash
# 1) .example 을 복사해 진짜 시크릿 파일을 만든다
cp chapters/ch04-config/backend/src/main/resources/application-local-secret.yml.example \
   chapters/ch04-config/backend/src/main/resources/application-local-secret.yml

# 2) .gitignore 가 차단하는지 확인
git check-ignore -v chapters/ch04-config/backend/src/main/resources/application-local-secret.yml
# → 차단된다는 출력이 떠야 함

# 3) 부팅 후 추적
curl "http://localhost:8080/api/config/source?key=anvil.sender.api-key"
# → source: "Config resource ... [application-local-secret.yml]" 으로 보임
```

핵심 파일:

| 파일 | 역할 |
|---|---|
| `resources/application.yml` | 공통 + 기본값 |
| `resources/application-{local,dev,prod}.yml` | 환경별 오버라이드 |
| `resources/application-local-secret.yml.example` | 시크릿 템플릿 (커밋됨) |
| `resources/application-local-secret.yml` | 진짜 시크릿 (.gitignore 차단) |
| `config/AppProperties.java` | `@ConfigurationProperties("anvil")` record |
| `secret/SecretMasker.java` | 키 이름으로 sensitive 판별 + 값 마스킹 |
| `inspect/ConfigInspector.java` | 살아있는 Environment 조회 |
| `notify/RateLimiter.java` | 환경별 한도 강제 |

## 5. 시각화 (See)

`frontend/index.html` — 활성 프로파일 뱃지 + 바인딩된 설정 표 + 환경 비교 표 + 키 추적기 + 연속 발송 데모.

```bash
./gradlew :chapters:ch04-config:bootRun                                  # 8080
python3 -m http.server 5173 --directory chapters/ch04-config/frontend    # 5173
# → http://localhost:5173
```

해보면 좋은 시나리오:
1. **연속 발송** 카드에서 5건 → 모두 200 (local 은 무제한 -1)
2. `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun` 으로 재기동 후 연속 11건 →
   1\~10 성공, 11번째 429 차단. UI 가 *환경에 따라 코드 0줄 수정으로* 동작이 달라지는 걸 시각화
3. **키 추적기** 에서 `anvil.sender.api-key` → 진짜 시크릿 파일을 만들기 전엔
   `application.yml` 출처(`PLACEHOLDER-...`), 만든 후엔 `application-local-secret.yml` 출처

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)

- [ ] **시크릿은 git 에 절대 두지 않음** — `.gitignore` 차단 + pre-commit 훅(gitleaks 등) 으로 이중 방어
- [ ] **PR 리뷰 체크리스트 1번에 "시크릿 포함 여부"** 명시
- [ ] **시크릿 회전(rotation) 절차 문서화** — 새벽 3시에 즉흥적으로 찾지 말 것
- [ ] **prod 의 `/actuator/env` 차단** — 본 챕터의 `application-prod.yml` 가 시연 (health, info 만 노출)
- [ ] **응답/로그 마스킹** — `SecretMasker` 패턴. 운영에선 Logback `MaskingConverter` 같은 걸로 일관 적용
- [ ] **`@ConfigurationProperties` 로 묶기** — `@Value` 산발 사용은 오타 / 누락이 런타임에 터짐. record 시그니처 = 환경에 기대하는 키 목록의 단일 출처
- [ ] **타입 검증** — record + `jakarta.validation` 어노테이션으로 음수/null 등 부적절한 값 차단 (ch06 에서 본격 다룸)
- [ ] **외부 API URL 도 환경별 분리** — 로컬에서 실서버 두드리지 못하게 (=실수 방지 + 비용)

## 7. 흔한 실수 & 디버깅

1. **`application.yml` 에 시크릿 적어두고 `// TODO 빼야 함`**
   "나중에" 가 오지 않는다. 처음부터 별도 파일 + `.gitignore`.

2. **환경변수가 안 먹는다 — kebab-case ↔ UPPER_SNAKE 매핑 모름**
   `anvil.sender.api-key` → `ANVIL_SENDER_API_KEY` (점/하이픈 → 언더스코어, 대문자).
   `/api/config/source?key=anvil.sender.api-key` 로 추적하면 어느 source 가 잡혔는지 보임.

3. **profile 활성화는 시그널이 명확해야 함**
   부팅 로그 첫 줄의 `The following 1 profile is active: "local"` 을 항상 확인.
   `/api/config/active` 의 `activeProfiles` 가 비어 있으면 default 가 활성.

4. **`@ConfigurationProperties` 가 안 바인딩됨**
   `@ConfigurationPropertiesScan` 또는 `@EnableConfigurationProperties(AppProperties.class)` 누락이 흔하다.
   본 챕터는 `Ch04Application` 에 `@ConfigurationPropertiesScan` 을 붙여 해결.

5. **`configurationProperties` 가 source 로 잡혀서 진짜 출처가 안 보임**
   Spring 이 조회 최적화를 위해 등록하는 의사-소스. 본 챕터 `ConfigInspector` 는 의도적으로 건너뛴다.

6. **시크릿 파일을 src/main/resources 에 두면 jar 에 포함**
   학습용 데모에선 OK 지만, 실 운영은 **외부 마운트** (Docker secrets, K8s Secret 볼륨, AWS Secrets Manager + Spring Cloud) 로 분리.

## 8. 더 깊이 (선택)

- Spring Boot 공식 — Externalized Configuration / Property Source Order
- 12 Factor App — III. Config (환경마다 다른 건 환경변수로)
- gitleaks / truffleHog — pre-commit / CI 단계 시크릿 스캔
- Spring Cloud Vault / AWS Secrets Manager / HashiCorp Vault — prod 시크릿 위임
- ConfigData API (Boot 2.4+) — `spring.config.import` 의 동작 원리
