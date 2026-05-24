package com.devsmith.anvil.ch09b.config;

import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학습용 스키마 초기화.
 *
 * 운영 코드라면 Flyway / Liquibase 가 정공법.
 * Lite 모드(codegen 없음)와 일관성을 위해 부팅 시 DDL 을 한 번에 실행한다.
 *
 * 매 부팅마다 DROP → CREATE → 시드. ch08/ch09 의 ddl-auto=create-drop 과 동일한 학습 모드.
 *
 * 함정: jOOQ 의 execute("DDL string") 은 트랜잭션 격리에서 잘 동작.
 *      여러 문장을 세미콜론으로 묶지 말고 한 문장씩 실행 — 드라이버에 따라 multi-statement 미지원.
 */
@Configuration
public class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ApplicationRunner initSchema(DSLContext dsl) {
        return args -> doInit(dsl);
    }

    @Transactional
    void doInit(DSLContext dsl) {
        // 의존 순서 역으로 drop (FK 때문)
        dsl.execute("DROP TABLE IF EXISTS comments CASCADE");
        dsl.execute("DROP TABLE IF EXISTS posts CASCADE");

        dsl.execute("""
                CREATE TABLE posts (
                    id          BIGSERIAL PRIMARY KEY,
                    title       VARCHAR(200) NOT NULL,
                    content     TEXT NOT NULL,
                    author      VARCHAR(50) NOT NULL,
                    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    -- ON CONFLICT 시연용 unique 제약 — (작성자, 제목) 은 유일
                    CONSTRAINT uq_posts_title_author UNIQUE (title, author)
                )
                """);
        dsl.execute("CREATE INDEX idx_posts_author     ON posts(author)");
        dsl.execute("CREATE INDEX idx_posts_created_at ON posts(created_at)");

        dsl.execute("""
                CREATE TABLE comments (
                    id          BIGSERIAL PRIMARY KEY,
                    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                    -- 자기 참조 — WITH RECURSIVE 트리 조회 시연용
                    parent_id   BIGINT REFERENCES comments(id) ON DELETE CASCADE,
                    author      VARCHAR(50) NOT NULL,
                    body        VARCHAR(500) NOT NULL,
                    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
                """);
        dsl.execute("CREATE INDEX idx_comments_post_id   ON comments(post_id)");
        dsl.execute("CREATE INDEX idx_comments_parent_id ON comments(parent_id)");

        log.info("[ch09b schema] posts + comments 테이블 초기화 완료");
    }
}
