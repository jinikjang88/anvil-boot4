package com.devsmith.anvil.ch09b.repository;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.devsmith.anvil.ch09b.schema.Tables.POSTS;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_AUTHOR;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_CONTENT;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_TITLE;

/**
 * ON CONFLICT DO UPDATE — ch09 QueryDSL 표준 미지원.
 *
 * 시나리오: (title, author) 가 같은 글이 이미 있으면 본문만 갱신, 없으면 새로 INSERT.
 * 단일 SQL 한 방. 동시성 안전 (PostgreSQL 의 ON CONFLICT 가 row-level lock 처리).
 *
 * SQL (PostgreSQL):
 *   INSERT INTO posts (title, content, author)
 *   VALUES (?, ?, ?)
 *   ON CONFLICT (title, author) DO UPDATE
 *     SET content = EXCLUDED.content
 *   RETURNING id
 *
 * JPA 로 흉내 내려면?
 *   - find → 있으면 update / 없으면 save 두 트랜잭션 라운드.
 *   - 동시성 시 race condition (둘 다 NotFound → 둘 다 INSERT → unique 위반 → 한쪽 예외).
 *   - 결국 native INSERT … ON CONFLICT 를 직접 짜는 게 정공법.
 */
@Repository
public class JooqUpsertRepository {

    private final DSLContext dsl;

    public JooqUpsertRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public long upsert(String title, String content, String author) {
        return dsl.insertInto(POSTS)
                .columns(POSTS_TITLE, POSTS_CONTENT, POSTS_AUTHOR)
                .values(title, content, author)
                .onConflict(POSTS_TITLE, POSTS_AUTHOR)
                .doUpdate()
                // EXCLUDED.content — INSERT 시도된 값. jOOQ 의 DSL.excluded() 가 일급 지원.
                .set(POSTS_CONTENT, DSL.excluded(POSTS_CONTENT))
                .returning(POSTS_ID)
                .fetchOne()
                .get(POSTS_ID);
    }
}
