# ch07-security — 보안 기초: JWT + BCrypt + AES + 인가

> 챕터 로드맵 및 컨벤션은 `.claude/CLAUDE.md` 참조.
> 본 챕터는 **학습용 데모**. 실 운영 배포 전엔 추가 점검 항목이 더 있다 ("실무 체크리스트" 참고).

## 1. 실생활 비유 (Why)

현관문 / 안방문 / 금고문. 보안은 한 겹이 아니다.

- **현관문(인증)** — *누구인지 확인* (이메일+비밀번호). 못 알아보면 못 들어옴 → 401
- **안방문(인가)** — *집안 어디까지 가도 되는가* (USER 거실까지, ADMIN 안방까지). 권한 부족 → 403
- **금고문(개인정보 암호화)** — 들어온 사람이라도 *원본을 볼 수는 없다*. 응답엔 마스킹, DB 엔 AES

세 가지가 *동시에* 필요하다. 하나라도 빠지면 다른 두 개의 가치가 줄어든다.

## 2. 진짜 현장 이야기 (War Story)

ISMS-P 심사 단골 지적사항 3 종 세트:

1. **패스워드 평문 저장 / MD5 / SHA-256 직접 해싱**
   "해시했으니 OK" 가 아니다. salt 없는 SHA 는 rainbow table 한 방. 정답은 **BCrypt (느린 해시 + salt 자동)**.

2. **JWT 만료(exp) 미설정 / 무한 토큰**
   "편의를 위해" 만료 안 둔 토큰이 유출되면 *영원히* 누가 쓰는지 모름. 만료 + 짧은 수명 + (옵션) refresh 토큰이 정답.

3. **개인정보 평문 저장**
   주민번호 / 전화번호 / 주소가 DB 에 평문이면 DB 덤프 한 방에 모두 노출. *민감 컬럼 AES + 응답 마스킹* 의 2단 방어가 표준.

본 챕터는 이 셋이 *동시에* 어떻게 살아 있어야 하는지를 작은 데모로 만져 본다.

## 3. 핵심 개념 (What)

| 비유 | 개념 | 본 챕터 구현 |
|---|---|---|
| 현관문 = 누구냐 | **Authentication** (인증) | JWT 검증 — `JwtAuthenticationFilter` |
| 안방문 = 어디까지 | **Authorization** (인가) | `@PreAuthorize("hasRole('ADMIN')")` |
| 비밀번호는 *돌이킬 수 없게* | **BCrypt** — 단방향 해시 + salt | `BCryptPasswordEncoder` |
| 토큰은 *짧은 수명* | JWT `exp` 클레임 | `application.yml` 의 `expiration-seconds` |
| 사용자 열거 방지 | 로그인 실패 메시지 통일 | `InvalidCredentialsException` 한 가지로 |
| 민감 컬럼은 *암호화 저장* | AES-256-GCM | `EncryptionService` |
| 응답엔 *원본 가리고* | 마스킹 | `EncryptionService.maskPhone` |

### 인증 / 인가의 차이

```
   클라이언트                Spring Security 필터                     컨트롤러
   ─────────                ────────────────────                     ─────────
   Authorization: Bearer ─→ JwtAuthenticationFilter          ─→
                            (인증 → SecurityContext 채움)
                                                             ─→     @PreAuthorize
                                                                    (인가 검사)
                                                             ─→     본문 실행

   토큰 없음/위조  →  EntryPoint → 401   (Authentication 실패)
   role 부족      →  AccessDeniedHandler → 403   (Authorization 실패)
```

### JWT 구조

```
header.payload.signature
─┬───── ─┬───── ─┬─────
 │       │       │
 │       │       └── HMAC-SHA256(base64url(header) + "." + base64url(payload), secret)
 │       └── { sub, iss, iat, exp, email, role } ← 클레임
 └── { alg: HS256, typ: JWT }
```

핵심: **payload 는 평문**. 누구나 base64 디코드해서 볼 수 있다. JWT 의 *기밀성* 은 없고 *위변조 방지* 만 있음. **민감 정보는 payload 에 절대 X**.

## 4. 코드로 벼리기 (How)

### 시크릿 파일 준비

```bash
cp chapters/ch07-security/backend/src/main/resources/application-local-secret.yml.example \
   chapters/ch07-security/backend/src/main/resources/application-local-secret.yml
# 위 예제 파일의 키를 그대로 써도 됨 (학습용). 운영은 다른 키 필수.

# 실제 키 생성 (운영용):
openssl rand -base64 32
```

### 실행 + 호출 시나리오

```bash
./gradlew :chapters:ch07-security:bootRun

# 1) 회원가입 (USER + ADMIN)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"u@x.com","password":"password123","name":"홍",
       "phoneNumber":"010-1234-5678","role":"USER"}'

curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"a@x.com","password":"adminpass123","name":"adm",
       "phoneNumber":"010-9999-8888","role":"ADMIN"}'

# 2) 로그인 → JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"u@x.com","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# 3) 토큰 없이 보호된 리소스 → 401
curl -i http://localhost:8080/api/v1/users/me

# 4) 토큰 있고 → 200 (마스킹된 phone)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me

# 5) USER 토큰으로 관리자 API → 403
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users

# 6) JWT 디코더 (서명 검증 후 클레임 반환)
curl -X POST http://localhost:8080/api/v1/auth/decode \
  -H 'Content-Type: application/json' \
  -d "{\"token\":\"$TOKEN\"}"

# 테스트
./gradlew :chapters:ch07-security:test
```

### 핵심 파일

| 파일 | 역할 |
|---|---|
| `config/SecurityConfig.java` | SecurityFilterChain + `@EnableMethodSecurity` + `BCryptPasswordEncoder` |
| `security/JwtService.java` | HS256 발급/검증, jjwt 0.12 (clock 어댑터로 테스트 결정성) |
| `security/JwtAuthenticationFilter.java` | `Authorization: Bearer` → SecurityContext 채움 |
| `security/JwtAuthenticationEntryPoint.java` | 401 ProblemDetail |
| `security/JwtAccessDeniedHandler.java` | 403 ProblemDetail |
| `security/EncryptionService.java` | AES-256-GCM 양방향 + `maskPhone` 정적 헬퍼 |
| `service/UserService.java` | register / authenticate (BCrypt + AES 적용) |
| `controller/AuthController.java` | register / login / decode |
| `controller/UserController.java` | `/me` 인증만 / `/users` `@PreAuthorize("hasRole('ADMIN')")` |

## 5. 시각화 (See)

`frontend/index.html` — 3-패널 레이아웃:
- **좌**: 회원가입 + 로그인 폼 + 응답 표시. 로그인 성공 시 토큰을 localStorage 에 저장
- **중**: JWT 디코더 — header / payload / signature 분해, 만료까지 카운트다운
- **우**: 보호된 엔드포인트 3 버튼 (`/me` 인증 / `/users` 관리자만 / `/me` 토큰 없이 → 401)

```bash
./gradlew :chapters:ch07-security:bootRun                                  # 8080
python3 -m http.server 5173 --directory chapters/ch07-security/frontend    # 5173
```

체험 시나리오:
1. USER 로 가입+로그인 → 상단 뱃지 "로그인됨 (이메일·USER)" → `/me` 200 / `/users` **403**
2. 로그아웃 후 ADMIN 으로 가입+로그인 → `/me` 200 / `/users` **200** (목록 2명)
3. 로그아웃 후 `/me` 호출 → **401**
4. JWT 디코더의 *payload* 가 평문임을 확인 — `sub`, `email`, `role`, `exp` 가 모두 보임

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)

- [ ] **비밀번호는 BCrypt** — `BCryptPasswordEncoder` 또는 Argon2. MD5/SHA 직접 사용 금지
- [ ] **BCrypt strength 검토** — 기본 10. 운영 부하에 따라 12 권장 (해싱 시간 ~100ms)
- [ ] **JWT 만료 강제** — `exp` 클레임 필수. refresh token 패턴은 별도 챕터 영역
- [ ] **JWT secret 회전 계획** — 운영 키는 정기적으로 회전. 회전 중에는 구/신 키로 검증 가능하게
- [ ] **JWT payload 에 민감정보 X** — 평문이라 어차피 보임. id / role 정도까지
- [ ] **개인정보 컬럼 AES 저장** — 주민번호 / 전화번호 / 주소. 응답엔 마스킹 (`SecretMasker` 패턴, ch04 와 동일)
- [ ] **AES 모드는 GCM** — CBC + 별도 HMAC 보다 안전, IV 매번 새로
- [ ] **로그에 비밀번호 / 토큰 흘리지 말 것** — 요청 본문 / 응답 본문 로깅 정책 점검 (ch15 관측성 영역)
- [ ] **CORS 는 origin 화이트리스트** — `allowedOrigins("*")` 절대 금지
- [ ] **인증 실패 메시지는 통일** — "이메일 존재 안함" vs "비번 틀림" 구분 X (열거 공격 방지)
- [ ] **rate limit + brute-force 차단** — 같은 IP 로 N회 실패 시 잠시 차단 (ch11 캐시 + ch15 메트릭 영역)

## 7. 흔한 실수 & 디버깅

1. **CSRF 켠 채로 POST 가 403**
   JWT 기반 stateless API 는 CSRF 보호가 (이 시나리오에선) 불필요. `csrf.disable()` 명시.
   브라우저 폼/세션 시나리오면 CSRF 켜야 함.

2. **`@PreAuthorize` 가 안 동작**
   `@EnableMethodSecurity` 누락. SecurityConfig 클래스 위에 붙이는지 확인.

3. **`hasRole('ADMIN')` 인데 권한 부여를 `"ADMIN"` 으로**
   `hasRole(X)` 는 `ROLE_X` 와 비교. 권한 등록 시 `ROLE_` prefix 필수.
   본 챕터의 `UserRole.asAuthority()` 가 이 컨벤션을 지킴.

4. **JWT 만료 검증이 테스트에서 비결정적**
   jjwt 의 기본 시계는 시스템 시계. 본 챕터처럼 `Clock` 어댑터 주입으로 테스트 결정성 확보.

5. **SecurityFilterChain 의 매칭 순서 함정**
   `requestMatchers` 는 *위에서 아래로* 매치. `anyRequest().permitAll()` 을 위에 두면 그 아래 규칙이 무시됨.

6. **`AuthenticationPrincipal` 가 null**
   필터에서 `SecurityContextHolder` 를 못 채웠거나, `principal` 이 다른 타입. 본 챕터는 String (userId).

7. **로그인 응답에서 token 이 너무 길어 로그가 망가짐**
   토큰을 응답 외 다른 곳에 echo 하면 안 됨. 로깅 마스킹.

## 8. 더 깊이 (선택)

- **OAuth2 / OIDC** — 소셜 로그인 / SSO. Spring Security 의 `oauth2Login()` + `oauth2ResourceServer()`
- **Refresh Token 패턴** — 짧은 access + 긴 refresh, 회전 시 invalidation
- **OWASP Top 10 — A02:2021 Cryptographic Failures**
- **OWASP ASVS** — 보안 검증 표준
- **KMS / HSM / Vault** — prod 시크릿 관리 위임 (ch04 의 연장선)
- **Token Introspection (RFC 7662)** — 토큰 상태를 IdP 에 물어보는 패턴
- **JWE (Encrypted JWT)** — payload 도 암호화해야 한다면 (드문 경우)
