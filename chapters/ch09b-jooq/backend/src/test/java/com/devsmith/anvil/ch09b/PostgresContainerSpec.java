package com.devsmith.anvil.ch09b;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 테스트 베이스 — 공용 PostgreSQL 16 컨테이너.
 *
 * 윈도우 함수 / WITH RECURSIVE / ON CONFLICT 는 PostgreSQL 방언이므로 진짜 컨테이너 필수.
 * H2 / Derby 같은 경량 DB 로는 일부 기능 자체가 안 됨.
 */
@Testcontainers
public abstract class PostgresContainerSpec {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("anvil")
            .withUsername("anvil")
            .withPassword("anvil");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // 시드는 테스트에서 직접 제어
        registry.add("anvil.seed.enabled", () -> "false");
    }
}
