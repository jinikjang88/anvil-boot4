package com.devsmith.anvil.ch09b.repository;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_AUTHOR;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_BODY;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_PARENT_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_POST_ID;

/**
 * 진짜 batch INSERT — JPA 영속성 컨텍스트 경유와 다른 점.
 *
 * SQL:
 *   INSERT INTO comments (post_id, parent_id, author, body)
 *   VALUES (?, ?, ?, ?), (?, ?, ?, ?), ..., (?, ?, ?, ?)
 *
 * 한 SQL 한 라운드. N rows.
 *
 * JPA 라면?
 *   - saveAll(...) 은 영속성 컨텍스트에 N개 등록 → flush 시 N개의 INSERT (또는 batch_size 묶음).
 *   - SEQUENCE 전략이면 id 확보를 위해 select nextval N번 (Hibernate 4 이전엔 더 심함).
 *   - 즉, JPA 의 "saveAll" 은 의미 있는 batch 가 아님 (튜닝 필요).
 *
 * 이 챕터에서는 200 rows 를 한 INSERT 로 시연 — 발행 SQL 1개.
 */
@Repository
public class JooqBulkInsertRepository {

    private final DSLContext dsl;

    public JooqBulkInsertRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public int bulkInsert(long postId, List<CommentInput> comments) {
        if (comments.isEmpty()) return 0;

        // .values(...).values(...).values(...) 체이닝 — 한 SQL 에 N rows.
        // var 사용: Tables.COMMENTS 의 Table<?> 와일드카드가 InsertValuesStep4 의 첫 타입 파라미터에 캡처되어
        //          명시적 타입을 쓰면 incompatible types 가 난다.
        var insert = dsl.insertInto(COMMENTS, COMMENTS_POST_ID, COMMENTS_PARENT_ID, COMMENTS_AUTHOR, COMMENTS_BODY)
                .values(postId, null, comments.get(0).author(), comments.get(0).body());
        for (int i = 1; i < comments.size(); i++) {
            CommentInput c = comments.get(i);
            insert = insert.values(postId, null, c.author(), c.body());
        }
        return insert.execute();
    }

    public record CommentInput(String author, String body) {}
}
