# ch03-di — 의존성 주입, 왜 `new` 쓰면 안 되나

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.

## 1. 실생활 비유 (Why)

치킨 시킬 때 직접 닭 잡으러 안 간다. 가게에 전화하고, 라이더가 가져온다.

서비스 코드 안에서 `new EmailChannel()` 을 박는 건 **닭 잡으러 양계장 가는 짓**. 가는 길도 모르고, 도착해서 잡는 법도 모르고, 정작 본업(치킨 먹기 = 비즈니스 로직)에 쓸 시간이 사라진다.

**DI(의존성 주입)** 는 가게에 전화하는 행위다. "치킨 한 마리 — 양념 반 후라이드 반" 하고 주문 명세(=인터페이스 + 생성자 시그니처)만 던지면, 라이더(=Spring)가 누구한테 시킬지 / 어떻게 만들지를 알아서 처리한다.

테스트 환경에선 *"가짜 가게(=mock)"* 한테 주문을 돌리면 된다. 같은 코드, 다른 가게.

## 2. 진짜 현장 이야기 (War Story)

신입 때 `UserService` 안에서 `EmailSender sender = new SmtpEmailSender(host, port, ...)` 를 박아뒀다. 잘 돌았다 — 운영 환경에선. 그러다 단위 테스트를 쓰라고 시키니, 회원가입 한 건 검증하려고 진짜 SMTP 서버를 띄워야 했다. CI 에선 SMTP 서버가 없어서 매번 시간 초과로 실패. 결국 새벽까지 모든 `new` 를 들어내고 인터페이스 + 생성자 주입으로 갈아엎었다. *"새 클래스 만드는 순간 `new` 부터 의심하라"* 가 그날의 교훈.

같은 함정은 클래스가 커질수록 더 깊어진다. `Clock`, `RestTemplate`, `Random`, `FileSystem` — 모두 의존성이다. 인스턴스를 직접 만드는 순간 *테스트가 외부 세계에 끌려간다*.

## 3. 핵심 개념 (What)

| 비유 | 개념 | 본 챕터의 사용처 |
|---|---|---|
| 가게에 전화 | **IoC** — 객체 생명주기 통제권을 컨테이너에 넘김 | Spring `@SpringBootApplication` 이 컴포넌트 스캔 시작 |
| 주문 명세 | **생성자 주입** — 의존성을 생성자 인자로 받음 | `NotificationService(formatter, router, clock)` |
| 단골 가게 등록 | **`@Component` / `@Service`** — 자동 스캔 빈 | `EmailChannel`, `SmsChannel`, `ChannelRouter` 등 |
| 메뉴 조립 공식 | **`@Configuration` + `@Bean`** — 우리가 소유 못 한 클래스나 로직 필요한 빈 | `AppConfig.systemClock()` ({@code java.time.Clock} 은 JDK 소유) |
| "치킨집들 다 등록" | **`Map<String, X>` 자동 수집** — 같은 인터페이스 모든 구현 한 번에 주입 | `ChannelRouter(Map<String, Channel>)` |

### 주입 3 방식

```java
// 1) 생성자 주입 — 추천 (final 가능, 불변, 누락 시 컴파일 에러, 순환 참조 컴파일 단계 차단)
@Service
class GoodService {
    private final Foo foo;
    GoodService(Foo foo) { this.foo = foo; }
}

// 2) Setter 주입 — 선택적 의존성에만 (필수 의존성은 생성자로)
@Service
class SetterService {
    private Foo foo;
    @Autowired void setFoo(Foo foo) { this.foo = foo; }
}

// 3) Field 주입 — 안티패턴. 단위 테스트가 곤란하고, final 불가, 순환참조가 런타임에야 터짐
@Service
class FieldService {
    @Autowired private Foo foo;          // ← 피하라
}
```

본 챕터의 `NotificationService` 는 **(1) 생성자 주입** 이고, 그래서 단위 테스트가 Spring 없이 굴러간다 (`NotificationServiceTest` 참조).

## 4. 코드로 벼리기 (How)

```bash
# 백엔드
./gradlew :chapters:ch03-di:bootRun

# 알림 발송
curl -X POST http://localhost:8080/api/notify \
  -H 'Content-Type: application/json' \
  -d '{"recipient":"user@example.com","subject":"hi","body":"hello"}'
# → { "recipient":"user@example.com","channel":"email", ... }

curl -X POST http://localhost:8080/api/notify \
  -H 'Content-Type: application/json' \
  -d '{"recipient":"01012345678","subject":"alert","body":"down"}'
# → { ..., "channel":"sms", ... }

# 빈 의존성 그래프 (D3 시각화의 데이터 소스)
curl http://localhost:8080/api/beans/graph | python3 -m json.tool

# 테스트
./gradlew :chapters:ch03-di:test
```

핵심 파일:

| 파일 | 역할 |
|---|---|
| `channel/Channel.java` (interface) + `EmailChannel`, `SmsChannel` | 전략 패턴, 각각 `@Component` |
| `channel/ChannelRouter.java` | `Map<String, Channel>` 자동 수집 데모 |
| `formatter/MessageFormatter.java` | 의존성 0개 — 그래프 잎 노드 |
| `service/NotificationService.java` | **생성자 주입의 정석** — 단위 테스트 가능 |
| `service/BeanGraphService.java` | `ApplicationContext` 를 리플렉션으로 들여다보며 그래프 생성 |
| `config/AppConfig.java` | `@Bean Clock` — `@Configuration + @Bean` 의 정당한 사용처 |

## 5. 시각화 (See)

`frontend/index.html` — D3.js force-directed 그래프로 실시간 빈 의존성 시각화 + 알림 발송 폼.

```bash
./gradlew :chapters:ch03-di:bootRun                                       # 8080
python3 -m http.server 5173 --directory chapters/ch03-di/frontend         # 5173
# → http://localhost:5173
```

상단 카드에서 알림 한 건 발송 → 로그에 채널 선택 흔적 확인. 하단 카드는 `/api/beans/graph` 응답을 D3 로 렌더링한다 — 노드를 드래그해 그래프 모양을 바꿔 보면 의존성 클러스터가 눈에 들어온다.

오행 컬러 매핑:
- **컨트롤러** = 검정 / **서비스** = 水 / **컴포넌트** = 木 / **컨피그** = 火 / **외부**(`Clock`) = 金

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)

- [ ] **모든 의존성은 생성자 주입** — `@Autowired` field 가 보이면 PR 코멘트
- [ ] **`final` 필드 강제** — 코드 리뷰에서 mutable dependency 가 보이면 의심
- [ ] **순환 참조 발견 시 도메인 설계가 잘못된 신호** — `@Lazy` 로 우회하지 말고 책임 분리
- [ ] **테스트가 컨텍스트를 띄워야만 돌아가면 적신호** — 단위 테스트 가능한지 점검 (POJO 조립으로 검증되어야 함)
- [ ] **`@Configuration` 의 빈 메서드는 멱등성 유지** — 동일 입력 동일 빈 보장 (Spring 이 캐시함, 부작용 코드 넣지 말 것)
- [ ] **개인정보가 흐르는 로깅** — `EmailChannel` 같은 채널 구현이 PII 를 로그에 그대로 찍지 않는지 점검 (본 챕터 데모는 학습용으로 그냥 찍음 — 운영 코드 아님)

## 7. 흔한 실수 & 디버깅

1. **순환 참조 — A 가 B 의존, B 가 A 의존**
   생성자 주입이면 컴파일은 통과해도 컨테이너 기동 시점에 `BeanCurrentlyInCreationException`. field 주입에선 운 좋게(?) 기동에 성공해도 NPE 가 런타임에 폭발. 해결책은 `@Lazy` 가 아니라 *도메인 분리* — 둘 사이의 공통 의존을 제 3 의 빈으로 추출.

2. **`@Component` 누락 → `NoSuchBeanDefinitionException`**
   메인 클래스 패키지 하위에 있는지, 어노테이션을 빼먹지 않았는지 확인. 디버그 팁: `getBean(MyService.class)` 가 실패하면 컴포넌트 스캔 범위부터 본다.

3. **같은 인터페이스의 빈이 여러 개라 `NoUniqueBeanDefinitionException`**
   본 챕터의 `Channel` 처럼. 해결책: ① `@Primary` ② `@Qualifier` ③ 본 챕터처럼 `Map`/`List` 로 모두 받아 직접 라우팅.

4. **`@Configuration` vs `@Component` 혼동**
   `@Configuration` 클래스 안의 `@Bean` 메서드는 *동일 빈 반환을 Spring 이 보장* (CGLIB 프록시). `@Component` 안에 `@Bean` 을 쓰면 매번 새 객체가 나온다. 빈을 정의하려면 `@Configuration` 을 쓰자.

5. **테스트에서 `new MyService(...)` 가 답답해서 `@SpringBootTest` 로 도망**
   먼저 의존성 트리부터 본다. 너무 많이 받으면 그 서비스 자체가 SRP 위반 — 가짜 7개 만드느니 진짜 SRP 리팩터를 하라는 신호.

## 8. 더 깊이 (선택)

- Spring 공식 문서 — Core / Beans / IoC Container
- *"Why Field Injection is Evil"* — Oliver Drotbohm (Spring committer)
- JSR 330 (`@Inject`) — 표준 DI 어노테이션, Spring 도 인식
- Effective Java Item 5 — "자원을 직접 명시하지 말고 의존 객체 주입을 사용하라" (Joshua Bloch)
