// ch08-jpa — JPA와 영속성 컨텍스트 — N+1 그 흔한 함정
plugins {
    id("anvil.spring-boot-conventions")
}

description = "ch08-jpa — JPA와 영속성 컨텍스트 — N+1 그 흔한 함정"

dependencies {
    // Spring Data JPA — JpaRepository / @Entity / @EntityGraph / @Transactional
    implementation(libs.spring.boot.starter.data.jpa)

    // PostgreSQL JDBC 드라이버 — 런타임만 필요
    runtimeOnly(libs.postgresql.driver)

    // 테스트 — Testcontainers PostgreSQL.
    // H2 는 의도적으로 안 씀: ORM 학습은 실제 DB 방언을 봐야 의미 있음.
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}
