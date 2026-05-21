# ch01-foundation — Spring Boot 4의 세계

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.

## 1. 실생활 비유 (Why)

스마트폰 OS 업데이트와 똑같다. iOS 17 → 18 로 올라간다고 폰이 바뀌는 건 아니지만,
배경 화면 위젯이 늘어나고, 화상 통화가 깔끔해지고, 오래된 앱이 일부 동작 안 한다.
Spring Boot 도 마찬가지로 3 → 4 가 되면 *같은 폰인데* 쓰는 방식이 조금씩 달라진다.

이 챕터는 그 "OS 업데이트" 가 실제로 무엇을 바꿨는지, 그리고 우리가 왜 또 새 버전을
배워야 하는지를 가장 단순한 "hello, anvil" 엔드포인트 하나로 만져 본다.

## 2. 진짜 현장 이야기 (War Story)

Boot **2 → 3** 마이그레이션 때 모두가 똑같은 곳에서 하루를 날렸다.
`javax.persistence` 가 `jakarta.persistence` 로 바뀐 거. import 한 줄씩 갈아끼우는 데
오전 다 갔고, 오후엔 빌드는 통과하는데 런타임 NoClassDefFoundError 가 줄줄이 터졌다.

Boot **3 → 4** 의 함정은 조금 다르다. 패키지 이동(`spring-boot-test-autoconfigure` 안의
`WebMvcTest` → `spring-boot-webmvc-test` 별도 모듈)이나 새 starter 분할(웹 스택 모듈화)
같은 *조용한 재구성* 이 곳곳에 있다. 메이저 버전 올릴 땐 starter 의 transitive 만 믿지 말고
실제 사용 import 가 어디 jar 에 들어 있는지 한 번 확인하는 게 안전하다.

## 3. 핵심 개념 (What)

| 비유 | Spring Boot 4 개념 |
|---|---|
| OS 업데이트 | 메이저 버전업 — javax→jakarta 같은 *대규모 단절* 은 끝났고, 모듈 재배치/JDK 25 정렬이 중심 |
| 폰 교체 없이 새 카메라 앱 | Java 25 LTS — Virtual Thread / Record / Pattern Matching 을 표준으로 가정 |
| 앱 호환성 경고 | 의존성 분리 — `WebMvcTest` 가 별도 모듈로 빠진 식의 패키지 이동 |
| 위젯 추가 | Actuator 기본 노출 / Observability 정렬 강화 |

이 챕터의 코드 자체는 단순하다. `GET /api/hello` 가 `{ "message": "hello, anvil" }` 을
반환한다. **이걸 띄울 수 있다는 것 자체가** Boot 4 + Java 25 toolchain + Gradle convention plugin
조합이 동작한다는 증명이다.

## 4. 코드로 벼리기 (How)

```bash
# 1) 백엔드 실행 (Java 25 toolchain 자동 사용)
./gradlew :chapters:ch01-foundation:bootRun

# 2) 호출
curl http://localhost:8080/api/hello
# → {"message":"hello, anvil"}

curl "http://localhost:8080/api/hello?name=%EC%9E%A5%EC%A7%84%EC%9D%B5"
# → {"message":"hello, 장진익"}
#
# 한글 파라미터는 반드시 URL 인코딩 — 인코딩 안 하면 Tomcat 이 400 으로 끊는다.

# 3) 테스트
./gradlew :chapters:ch01-foundation:test
```

핵심 파일:

| 파일 | 역할 |
|---|---|
| `backend/src/main/java/.../Ch01Application.java` | `@SpringBootApplication` 진입점 |
| `.../service/HelloService.java` | 도메인 로직 (null/blank 처리 — 컨트롤러로 NPE 안 새도록) |
| `.../controller/HelloController.java` | `@RestController` + `record HelloResponse` (Java 25 record 직렬화) |
| `src/main/resources/application.yml` | 포트 8080, actuator health/info 노출 |

## 5. 시각화 (See)

`frontend/index.html` — Tailwind Play CDN + 오행 팔레트로 만든 데모 카드.
이름 입력 → `호출` → `fetch('http://localhost:8080/api/hello')` → JSON 결과 표시.
오행 컬러로 상태 구분: **木**(녹) = 성공, **火**(적) = 에러, **金**(회) = 대기.

```bash
# 두 개 터미널 또는 백그라운드:
./gradlew :chapters:ch01-foundation:bootRun          # 1) 백엔드 (8080)
python3 -m http.server 5173 \
  --directory chapters/ch01-foundation/frontend       # 2) 프론트 (5173)

# 브라우저: http://localhost:5173
```

> 백엔드가 8080 이 아닌 다른 포트라면 `frontend/script.js` 의 `API` 상수만 바꾸면 된다.
> CORS 는 `LocalDevCorsConfig` 가 `local` 프로필일 때만 5173 origin 을 허용한다 —
> 실 운영 보안은 ch07-security 에서 본격적으로 다룸.

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)

- [ ] JDK toolchain 명시 — local/CI 가 같은 25.x 마이너 버전을 쓰는지 확인 (`./gradlew --version`)
- [ ] `application-*.yml` 분리 — 시크릿(`application-local-secret.yml`)은 `.gitignore` 로 차단됨을 검증
- [ ] `/actuator` 의 노출 endpoint 를 `health,info` 외에는 prod 에서 꺼 두기 (env / metrics 노출 시 정보 누설)
- [ ] 응답 DTO 는 `record` — 불변이므로 동시성/직렬화 일관성 보장
- [ ] 입력 파라미터의 null/blank/공백 처리는 *도메인 계층* 에서 — 컨트롤러는 HTTP 변환만 담당

## 7. 흔한 실수 & 디버깅

1. **`@MockBean` 그대로 써서 deprecation 경고**
   Boot 3.4+ 부터 `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`) 이 정식.
   Boot 4 에선 더더욱. 새로 쓰는 코드는 `@MockitoBean` 으로.

2. **`WebMvcTest` import 가 안 잡힘**
   Boot 4 에서 `spring-boot-test-autoconfigure` 가 분할됐다. 추가로
   `spring-boot-webmvc-test` 의존성이 필요하다. 본 레포는 `anvil.spring-boot-conventions` 가
   이미 포함한다 — convention plugin 만 적용하면 신경 안 써도 됨.

3. **`./gradlew bootRun` 이 toolchain 못 찾음**
   로컬에 JDK 25 가 없으면 Gradle Foojay resolver 가 다운로드하려 한다. 사내망/제한된
   환경에서는 미리 OpenJDK 25 를 설치하거나 (`apt install openjdk-25-jdk`), 회사 미러를
   `org.gradle.java.installations.paths` 로 지정한다.

4. **한글 쿼리 파라미터 400**
   `curl` 로 raw 한글을 URL 에 넣으면 Tomcat 이 거절한다 — 반드시 percent-encoding.
   브라우저 / 정상 HTTP 클라이언트는 자동으로 인코딩하므로 운영 코드에선 보통 문제 없음.

## 8. 더 깊이 (선택)

- Spring Boot 4.0 Release Notes — 모듈 재배치 표
- JEP 444 Virtual Threads — 다음 챕터(ch02) 핵심 주제
- Gradle Toolchains 문서 — Foojay vs 사내 미러 설정
