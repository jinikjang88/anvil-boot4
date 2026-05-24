package com.devsmith.anvil.ch09b.repository;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static com.devsmith.anvil.ch09b.schema.Tables.POSTS;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_AUTHOR;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_CREATED_AT;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_TITLE;
import static org.jooq.impl.DSL.rowNumber;

/**
 * 윈도우 함수 — ch09 QueryDSL 로는 안 되는 영역.
 *
 * SQL:
 *   SELECT id, title, author, created_at,
 *          ROW_NUMBER() OVER (PARTITION BY author ORDER BY created_at DESC) AS rn_in_author
 *   FROM posts
 *
 * 의미: "작성자별 가장 최근 글에서부터 1, 2, 3 …" 순위.
 *       "각 작성자의 최신 N개" 같은 통계가 한 쿼리로 끝.
 *
 * QueryDSL 로는?
 *   - 표준 API 미지원. native query (@Query nativeQuery=true) 로 우회하거나 SqlFunctions 패치.
 *   - 즉, 타입 세이프가 깨진다.
 *
 * jOOQ:
 *   - rowNumber().over(partitionBy(...).orderBy(...)) — 일급 표현식. 컴파일 타임 검증.
 *   - 다른 윈도우 함수도 동일 (RANK, DENSE_RANK, LAG, LEAD, SUM OVER, ...).
 */
@Repository
public class JooqRankingRepository {

    private final DSLContext dsl;

    public JooqRankingRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional(readOnly = true)
    public List<AuthorRanking> rankPostsByAuthor(int topNPerAuthor) {
        // 윈도우 함수 표현식 — jOOQ 의 일급 시민.
        Field<Integer> rn = rowNumber()
                .over()
                .partitionBy(POSTS_AUTHOR)
                .orderBy(POSTS_CREATED_AT.desc())
                .as("rn_in_author");

        // PostgreSQL 은 QUALIFY 키워드가 없으므로 nested select 로 처리.
        //   SELECT * FROM (
        //     SELECT ..., ROW_NUMBER() OVER (...) AS rn FROM posts
        //   ) ranked WHERE rn <= ?
        // jOOQ 는 .asTable() 로 인라인 서브쿼리를 깔끔히 표현.
        Table<?> ranked = dsl
                .select(POSTS_ID, POSTS_TITLE, POSTS_AUTHOR, POSTS_CREATED_AT, rn)
                .from(POSTS)
                .asTable("ranked");

        // 바깥 select 에서 별칭 컬럼을 참조 — Lite 모드에선 field 이름으로 다시 잡는다.
        Field<Long>           outerId        = ranked.field(POSTS_ID);
        Field<String>         outerTitle     = ranked.field(POSTS_TITLE);
        Field<String>         outerAuthor    = ranked.field(POSTS_AUTHOR);
        Field<OffsetDateTime> outerCreatedAt = ranked.field(POSTS_CREATED_AT);
        Field<Integer>        outerRn        = DSL.field(DSL.name("ranked", "rn_in_author"), Integer.class);

        return dsl.select(outerId, outerTitle, outerAuthor, outerCreatedAt, outerRn)
                .from(ranked)
                .where(outerRn.le(topNPerAuthor))
                .orderBy(outerAuthor.asc(), outerRn.asc())
                .fetch(r -> new AuthorRanking(
                        r.get(outerId),
                        r.get(outerTitle),
                        r.get(outerAuthor),
                        r.get(outerCreatedAt),
                        r.get(outerRn)
                ));
    }

    public record AuthorRanking(
            Long id,
            String title,
            String author,
            OffsetDateTime createdAt,
            int rankInAuthor
    ) {}
}
