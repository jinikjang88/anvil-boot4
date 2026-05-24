// ch09b-jooq — jOOQ — SQL-First 비교 세부챕터
//
// 학습 모드: Lite (codegen 없이) — DSL.table / DSL.field 로 직접 짠다.
// jOOQ 정공법(codegen + Flyway)은 README §8 의 build.gradle.kts 스니펫으로 설명.
//
// 의도: ch09 (QueryDSL) 가 막히는 영역 — 윈도우 함수, CTE/WITH RECURSIVE, ON CONFLICT,
//      진짜 batch INSERT — 를 jOOQ 한 줄로 보여주는 게 챕터의 목적.
plugins {
    id("anvil.spring-boot-conventions")
}

description = "ch09b-jooq — jOOQ — SQL-First 비교 세부챕터"

dependencies {
    // jOOQ 스타터 — DSLContext 자동 설정, DataSource 와 연결.
    implementation(libs.spring.boot.starter.jooq)

    // PostgreSQL JDBC 드라이버 — 런타임만 필요
    runtimeOnly(libs.postgresql.driver)

    // 테스트 — Testcontainers PostgreSQL.
    // jOOQ 도 결국 SQL — 실제 DB 방언(윈도우 함수, ON CONFLICT, WITH RECURSIVE)을 봐야 의미가 있음.
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}
