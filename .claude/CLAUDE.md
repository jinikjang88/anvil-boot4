# anvil-boot4

대장간(DevSmith) 브랜드의 Spring Boot 4 + Java 25 학습 레포지토리.
에북 "대장간 백엔드"의 16챕터 커리큘럼을 실습 코드로 구현한다.

## 스택
- Java 25 (LTS)
- Spring Boot 4.x
- Gradle 8.x (Kotlin DSL) + Version Catalog
- JUnit 5, AssertJ, Testcontainers
- PostgreSQL 16, Redis 7, Kafka (챕터별 선택)

## 구조 원칙
- `chapters/chXX-*` 는 각각 독립 빌드 가능한 서브모듈
- 각 챕터는 README + 실행 가능한 main + 테스트가 반드시 존재
- 챕터 간 의존 금지 (학습자가 챕터 단위로 clone/실행 가능해야 함)
- 공통 유틸이 필요하면 `common/` 으로 분리

## 코드 컨벤션
- 한글 주석 사용 (학습 목적)
- 테스트: given-when-then 패턴, 메서드명 한글 백틱 허용
- SOLID, Design Pattern 명시적 적용 — 어떤 패턴을 왜 썼는지 주석으로
- 예외 처리와 엣지 케이스를 명시적으로
- 보안: SQL Injection, XSS, 개인정보 암호화 항상 고려 (ISMS-P 관점)

## 환경
- application-local.yml / application-dev.yml / application-prod.yml 분리
- 로컬 의존성은 `docker/docker-compose.local.yml` 로 일괄 기동
- AWS 의존성은 LocalStack 사용

## 작업 규칙 (클로드 코드용)
- 새 챕터 생성 시 `scripts/new-chapter.sh` 사용 또는 동일 구조 복제
- 라이브러리 버전 추가는 반드시 `gradle/libs.versions.toml` 에 등록 후 참조
- 커밋 메시지는 Conventional Commits + 챕터 prefix: `feat(ch03): JPA 영속성 컨텍스트 예제 추가`
- 큰 변경 전에는 계획을 먼저 제시하고 승인 대기
