package com.devsmith.anvil.ch09b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ch09b-jooq 진입점.
 *
 * ch09 (QueryDSL) 의 자매편 — 같은 도메인을 SQL-First 로 다시 짠다.
 *
 * 시연 영역 (ch09 QueryDSL 로는 안 되거나 어색한 것들):
 *  - 윈도우 함수: ROW_NUMBER() OVER (PARTITION BY ...)
 *  - WITH RECURSIVE: 댓글 트리
 *  - ON CONFLICT DO UPDATE: PostgreSQL upsert
 *  - 진짜 batch INSERT (한 SQL 에 N rows)
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Ch09bApplication {
    public static void main(String[] args) {
        SpringApplication.run(Ch09bApplication.class, args);
    }
}
