package com.devsmith.anvil.ch09b.repository;

import com.devsmith.anvil.ch09b.search.PostSearchCondition;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS;
import static com.devsmith.anvil.ch09b.schema.Tables.COMMENTS_POST_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_AUTHOR;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_CONTENT;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_CREATED_AT;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_ID;
import static com.devsmith.anvil.ch09b.schema.Tables.POSTS_TITLE;

/**
 * ch09 (QueryDSL) 의 동일 검색을 jOOQ 로 다시 짠 비교군.
 *
 * 차이점:
 *  - jOOQ 의 Condition 은 함수형으로 조립 — QueryDSL 의 BooleanExpression 과 매우 닮음.
 *  - 둘 다 "조건이 null/no-op 이면 자동 무시" 패턴.
 *  - jOOQ 는 DSL.noCondition() 으로 명시적 no-op. (QueryDSL 은 그냥 null 반환.)
 *
 * 검색만 보면 두 도구는 비슷하다. 진짜 차이는 다음 클래스들 (Ranking, Tree, Upsert).
 */
@Repository
public class JooqPostSearchRepository {

    private final DSLContext dsl;

    public JooqPostSearchRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional(readOnly = true)
    public List<PostRow> search(PostSearchCondition cond, int page, int size) {
        Condition where = buildWhere(cond);

        return dsl.select(POSTS_ID, POSTS_TITLE, POSTS_CONTENT, POSTS_AUTHOR, POSTS_CREATED_AT)
                .from(POSTS)
                .where(where)
                .orderBy(POSTS_CREATED_AT.desc())
                .limit(size)
                .offset((long) page * size)
                .fetch(r -> new PostRow(
                        r.get(POSTS_ID),
                        r.get(POSTS_TITLE),
                        r.get(POSTS_CONTENT),
                        r.get(POSTS_AUTHOR),
                        r.get(POSTS_CREATED_AT)
                ));
    }

    @Transactional(readOnly = true)
    public int count(PostSearchCondition cond) {
        Integer n = dsl.selectCount().from(POSTS).where(buildWhere(cond)).fetchOne(0, Integer.class);
        return n == null ? 0 : n;
    }

    /**
     * 조건 빌더 — 각 조건은 null 이면 noCondition() 으로 무시.
     *
     * QueryDSL 의 BooleanExpression 과 비교:
     *   QueryDSL: private BooleanExpression authorEq(String a) { return a != null ? QPost.post.author.eq(a) : null; }
     *   jOOQ:     private Condition       authorEq(String a) { return a != null ? POSTS_AUTHOR.eq(a) : DSL.noCondition(); }
     *
     * 거의 같다. 검색 같은 쿼리에서는 둘이 막상막하.
     */
    private Condition buildWhere(PostSearchCondition cond) {
        List<Condition> conditions = new ArrayList<>();

        if (StringUtils.hasText(cond.keyword())) {
            String kw = "%" + cond.keyword().toLowerCase() + "%";
            conditions.add(DSL.lower(POSTS_TITLE).like(kw).or(DSL.lower(POSTS_CONTENT).like(kw)));
        }
        if (StringUtils.hasText(cond.author())) {
            conditions.add(POSTS_AUTHOR.eq(cond.author()));
        }
        if (cond.from() != null) {
            conditions.add(POSTS_CREATED_AT.ge(cond.from()));
        }
        if (cond.to() != null) {
            conditions.add(POSTS_CREATED_AT.lt(cond.to()));
        }
        if (cond.hasComments() != null) {
            // exists 서브쿼리 — jOOQ 의 DSL.exists() / notExists() 가 그대로 쓰임.
            Condition any = DSL.exists(DSL.selectOne().from(COMMENTS).where(COMMENTS_POST_ID.eq(POSTS_ID)));
            conditions.add(cond.hasComments() ? any : any.not());
        }

        return conditions.isEmpty() ? DSL.noCondition() : DSL.and(conditions);
    }
}
