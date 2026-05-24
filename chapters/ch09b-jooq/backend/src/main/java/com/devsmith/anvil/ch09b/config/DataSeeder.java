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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_AUTHOR;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_BODY;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_PARENT_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_POST_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_AUTHOR;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_CONTENT;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_CREATED_AT;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_TITLE;

/**
 * 학습용 시드.
 *  - 작성자 4명 × 시간대 3종 → 윈도우 함수(작성자별 랭킹) 시연 충분
 *  - 일부 게시글은 댓글 트리 (parent ↔ child 2단계) → WITH RECURSIVE 시연
 *  - 일부는 댓글 없음 → exists 필터 시연
 *
 * @Order(LOWEST_PRECEDENCE) — SchemaInitializer 의 ApplicationRunner 가 먼저 동작하도록.
 *   (둘 다 ApplicationRunner 면 순서가 모호하므로 명시.)
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    ApplicationRunner seed(SeedProperties props, DSLContext dsl) {
        return args -> {
            if (!props.enabled()) return;
            // 이미 시드되어 있으면 skip
            Integer count = dsl.selectCount().from(POSTS).fetchOne(0, Integer.class);
            if (count != null && count > 0) return;
            doSeed(dsl);
        };
    }

    @Transactional
    void doSeed(DSLContext dsl) {
        OffsetDateTime now       = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime yesterday = now.minus(1, ChronoUnit.DAYS);
        OffsetDateTime lastWeek  = now.minus(7, ChronoUnit.DAYS);

        // 게시글 — id 를 RETURNING 으로 받아 댓글 연결
        long p1 = insertPost(dsl, "jOOQ 시작하기",   "SQL-First 패러다임",            "민지", now);
        long p2 = insertPost(dsl, "WITH RECURSIVE",  "댓글 트리를 한 쿼리로",         "민지", now);
        long p3 = insertPost(dsl, "윈도우 함수",     "ROW_NUMBER OVER PARTITION BY",  "준호", now);
        long p4 = insertPost(dsl, "ON CONFLICT",     "PostgreSQL upsert 정공법",      "준호", yesterday);
        long p5 = insertPost(dsl, "batch INSERT",    "한 SQL 에 N rows",              "지훈", yesterday);
        long p6 = insertPost(dsl, "ORM vs SQL-First","jOOQ 의 선택 기준",              "지훈", yesterday);
        long p7 = insertPost(dsl, "codegen 워크플로","Flyway + nu.studer.jooq",        "수아", lastWeek);
        long p8 = insertPost(dsl, "Spring TX 통합",  "@Transactional + DSLContext",   "수아", lastWeek);

        // 댓글 트리 — p2 (WITH RECURSIVE 시연용) 에 깊이 2단계 트리
        long c1 = insertComment(dsl, p2, null, "anon",   "트리가 한 방에 됩니다?");
        long c2 = insertComment(dsl, p2, c1,   "민지",   "네, recursive 로요");
        long c3 = insertComment(dsl, p2, c2,   "anon",   "오 신기");
        long c4 = insertComment(dsl, p2, null, "준호",   "Java 로는 ORM 한계");
        insertComment(dsl,        p2, c4,   "민지",   "맞아요, 그래서 jOOQ");

        // 다른 게시글은 1-depth 댓글만
        insertComment(dsl, p1, null, "anon", "좋은 글");
        insertComment(dsl, p3, null, "수아", "랭킹 보고 싶어요");
        insertComment(dsl, p3, null, "지훈", "PARTITION BY 멋짐");
        insertComment(dsl, p4, null, "anon", "upsert 진짜 필요했음");
        insertComment(dsl, p6, null, "민지", "둘 다 쓰는 게 정답");

        // p5, p7, p8 은 일부러 댓글 없음 (필터 시연)
        log.info("[ch09b seed] 게시글 8건 + 댓글 트리(2단계 포함) 시드 완료");
    }

    private long insertPost(DSLContext dsl, String title, String content, String author, OffsetDateTime createdAt) {
        return dsl.insertInto(POSTS)
                .columns(POSTS_TITLE, POSTS_CONTENT, POSTS_AUTHOR, POSTS_CREATED_AT)
                .values(title, content, author, createdAt)
                .returning(POSTS_ID)
                .fetchOne()
                .get(POSTS_ID);
    }

    private long insertComment(DSLContext dsl, long postId, Long parentId, String author, String body) {
        return dsl.insertInto(COMMENTS)
                .columns(COMMENTS_POST_ID, COMMENTS_PARENT_ID, COMMENTS_AUTHOR, COMMENTS_BODY)
                .values(postId, parentId, author, body)
                .returning(COMMENTS_ID)
                .fetchOne()
                .get(COMMENTS_ID);
    }
}
