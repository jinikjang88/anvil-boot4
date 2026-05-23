package com.devsmith.anvil.ch08;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 테스트 베이스 — 모든 통합 테스트가 공용 PostgreSQL 컨테이너 한 개를 공유.
 * Testcontainers 는 임의 포트 매핑이라 docker-compose 의 5433 과 충돌하지 않음.
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
