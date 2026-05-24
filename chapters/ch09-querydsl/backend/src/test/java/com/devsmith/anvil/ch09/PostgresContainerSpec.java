package com.devsmith.anvil.ch09;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 테스트 베이스 — 모든 통합 테스트가 공용 PostgreSQL 16 컨테이너 한 개를 공유.
 *
 * QueryDSL 도 결국 발행하는 건 SQL — 실제 PostgreSQL 방언으로 검증해야 like 의 대소문자
 * 처리, exists 서브쿼리 형태, count 쿼리 형태가 맞는지 확인할 수 있다.
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
        // 테스트는 직접 시드.
        registry.add("anvil.seed.enabled", () -> "false");
    }
}
