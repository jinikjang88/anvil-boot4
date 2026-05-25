// ch09b-jooq — jOOQ — SQL-First 비교 세부챕터
//
// ── 두 가지 모드 ──
//
// [Lite (기본)] DSL.table / DSL.field 로 직접 짠다. codegen 없이 즉시 빌드.
//   → schema/Tables.java 에 컬럼 메타 수동 정의.
//   → ./gradlew :chapters:ch09b-jooq:build
//
// [Full (opt-in)] nu.studer.jooq 플러그인으로 DB 스키마 → 클래스 자동 생성.
//   → 사전 조건: PostgreSQL 이 localhost:5433 에 떠있고, bootRun 한 번 해서 스키마가 있어야 함.
//   → ./gradlew :chapters:ch09b-jooq:generateJooq -Pcodegen
//   → 생성물: build/generated/jooq/com/devsmith/anvil/ch09b/generated/
//   → Lite 의 schema/Tables.java 와 diff 비교해 "codegen 의 가치" 를 눈으로 보자.
//
// 의도: ch09 (QueryDSL) 가 막히는 영역 — 윈도우 함수, CTE/WITH RECURSIVE, ON CONFLICT,
//      진짜 batch INSERT — 를 jOOQ 한 줄로 보여주는 게 챕터의 목적.
plugins {
    id("anvil.spring-boot-conventions")
    alias(libs.plugins.jooq.codegen)
}

description = "ch09b-jooq — jOOQ — SQL-First 비교 세부챕터"

// ── codegen 기본 비활성화 ─────────────────────────────────────────────────
// 일반 빌드(./gradlew build)에서는 generateJooq 가 실행되지 않는다.
// 학습자가 명시적으로 -Pcodegen 을 넘길 때만 활성화.
// DB(localhost:5433) 가 없으면 ConnectionRefused 로 실패하므로 기본 off.
// jooq codegen task 기본 비활성화 — -Pcodegen 으로만 활성화.
tasks.configureEach {
    if (name.lowercase().contains("jooq")) {
        enabled = project.hasProperty("codegen")
    }
}

// ── jOOQ codegen 설정 ────────────────────────────────────────────────────
// 사전 조건:
//   1. docker compose -f docker/docker-compose.local.yml up -d postgres
//   2. ./gradlew :chapters:ch09b-jooq:bootRun  (SchemaInitializer 가 테이블 생성)
//   3. ./gradlew :chapters:ch09b-jooq:generateMainJooq -Pcodegen
//
// 생성된 클래스는 build/generated/jooq/ — VCS 제외 대상 (build 산출물).
// Lite 의 schema/Tables.java 와 diff 비교하면 "codegen 이 왜 실무 정공법인가" 가 명확.
jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5433/anvil"
                    user = "anvil"
                    password = "anvil"
                }
                generator.apply {
                    name = "org.jooq.codegen.JavaGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        includes = "posts|comments"
                        inputSchema = "public"
                    }
                    target.apply {
                        packageName = "com.devsmith.anvil.ch09b.generated"
                        directory = "build/generated/jooq"
                    }
                }
            }
        }
    }
}

dependencies {
    // jOOQ 스타터 — DSLContext 자동 설정, DataSource 와 연결.
    implementation(libs.spring.boot.starter.jooq)

    // PostgreSQL JDBC 드라이버 — 런타임 + codegen 시 DB 접속용.
    runtimeOnly(libs.postgresql.driver)
    jooqGenerator(libs.postgresql.driver)

    // 테스트 — Testcontainers PostgreSQL.
    // jOOQ 도 결국 SQL — 실제 DB 방언(윈도우 함수, ON CONFLICT, WITH RECURSIVE)을 봐야 의미가 있음.
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}
