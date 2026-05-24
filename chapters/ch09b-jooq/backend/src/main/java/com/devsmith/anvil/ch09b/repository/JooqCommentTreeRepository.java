package com.devsmith.anvil.ch09b.repository;

import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_AUTHOR;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_BODY;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_PARENT_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_POST_ID;

/**
 * WITH RECURSIVE — ch09 QueryDSL 로는 안 되는 영역.
 *
 * 시나리오: 한 게시글의 댓글 트리 (parent ↔ child) 를 한 SQL 에 깊이/순서까지 포함해 가져온다.
 *
 * SQL (PostgreSQL):
 *   WITH RECURSIVE tree AS (
 *     -- anchor: 루트 댓글 (parent_id IS NULL)
 *     SELECT id, parent_id, author, body, 1 AS depth
 *     FROM comments
 *     WHERE post_id = ? AND parent_id IS NULL
 *
 *     UNION ALL
 *
 *     -- recursive: 자식 댓글
 *     SELECT c.id, c.parent_id, c.author, c.body, t.depth + 1
 *     FROM comments c
 *     JOIN tree t ON c.parent_id = t.id
 *   )
 *   SELECT * FROM tree ORDER BY depth, id
 *
 * QueryDSL 로는?
 *   - WITH 자체가 표준 미지원. native query 외 방법 없음.
 *   - native 로 가면 결과 매핑은 다시 수동.
 *
 * jOOQ:
 *   - withRecursive(name).as(anchor.union(recursive)) — 일급 표현식.
 *   - 컴파일러가 컬럼 타입 검증.
 */
@Repository
public class JooqCommentTreeRepository {

    private final DSLContext dsl;

    public JooqCommentTreeRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional(readOnly = true)
    public List<CommentNode> tree(long postId) {
        // CTE 컬럼 시그니처 — anchor 와 recursive 의 select 타입이 일치해야 한다.
        Field<Long>    cteId       = DSL.field(DSL.name("id"),        SQLDataType.BIGINT.notNull());
        Field<Long>    cteParentId = DSL.field(DSL.name("parent_id"), SQLDataType.BIGINT);
        Field<String>  cteAuthor   = DSL.field(DSL.name("author"),    SQLDataType.VARCHAR(50).notNull());
        Field<String>  cteBody     = DSL.field(DSL.name("body"),      SQLDataType.VARCHAR(500).notNull());
        Field<Integer> cteDepth    = DSL.field(DSL.name("depth"),     SQLDataType.INTEGER.notNull());

        CommonTableExpression<?> tree = DSL.name("tree")
                .fields("id", "parent_id", "author", "body", "depth")
                .as(
                        // anchor
                        DSL.select(COMMENTS_ID, COMMENTS_PARENT_ID, COMMENTS_AUTHOR, COMMENTS_BODY,
                                   DSL.val(1))
                                .from(COMMENTS)
                                .where(COMMENTS_POST_ID.eq(postId))
                                .and(COMMENTS_PARENT_ID.isNull())

                                .unionAll(
                                        // recursive
                                        DSL.select(COMMENTS_ID, COMMENTS_PARENT_ID, COMMENTS_AUTHOR, COMMENTS_BODY,
                                                   cteDepth.plus(1))
                                                .from(COMMENTS)
                                                .join(DSL.table(DSL.name("tree")))
                                                .on(COMMENTS_PARENT_ID.eq(cteId))
                                )
                );

        return dsl.withRecursive(tree)
                .select(cteId, cteParentId, cteAuthor, cteBody, cteDepth)
                .from(tree)
                .orderBy(cteDepth.asc(), cteId.asc())
                .fetch(r -> new CommentNode(
                        r.get(cteId),
                        r.get(cteParentId),
                        r.get(cteAuthor),
                        r.get(cteBody),
                        r.get(cteDepth)
                ));
    }

    public record CommentNode(
            Long id,
            Long parentId,
            String author,
            String body,
            int depth
    ) {}
}
