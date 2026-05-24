package com.devsmith.anvil.ch09b.schema;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.time.OffsetDateTime;

/**
 * Lite 모드의 테이블/필드 메타.
 *
 * codegen 을 안 쓰는 대신 (Full 모드 정공법) — DSL.table / DSL.field 를 여기서 한 번 정의하고
 * 모든 리포지토리가 이 참조를 사용한다.
 *
 * Full 모드 정공법과 비교:
 *  - codegen 은 DB 스키마 → Tables / Posts / Comments / POJO / Indexes / Keys 까지 클래스로.
 *  - Lite 는 컬럼명 오타를 컴파일러가 못 잡는 단점이 있다. 대신 빌드 단순.
 *
 * 학습 의도: 같은 도메인을 두 모드(codegen / Lite) 로 봐도 jOOQ 의 핵심 — 표현력 — 은 동일.
 */
public final class Tables {

    private Tables() {}

    // ─── posts ──────────────────────────────────────────────────────────────
    public static final Table<?> POSTS = DSL.table(DSL.name("posts"));

    public static final Field<Long>           POSTS_ID         = DSL.field(name("posts", "id"),         SQLDataType.BIGINT.notNull());
    public static final Field<String>         POSTS_TITLE      = DSL.field(name("posts", "title"),      SQLDataType.VARCHAR(200).notNull());
    public static final Field<String>         POSTS_CONTENT    = DSL.field(name("posts", "content"),    SQLDataType.CLOB.notNull());
    public static final Field<String>         POSTS_AUTHOR     = DSL.field(name("posts", "author"),     SQLDataType.VARCHAR(50).notNull());
    public static final Field<OffsetDateTime> POSTS_CREATED_AT = DSL.field(name("posts", "created_at"), SQLDataType.TIMESTAMPWITHTIMEZONE.notNull());

    // ─── comments ───────────────────────────────────────────────────────────
    public static final Table<?> COMMENTS = DSL.table(DSL.name("comments"));

    public static final Field<Long>           COMMENTS_ID         = DSL.field(name("comments", "id"),         SQLDataType.BIGINT.notNull());
    public static final Field<Long>           COMMENTS_POST_ID    = DSL.field(name("comments", "post_id"),    SQLDataType.BIGINT.notNull());
    public static final Field<Long>           COMMENTS_PARENT_ID  = DSL.field(name("comments", "parent_id"),  SQLDataType.BIGINT);
    public static final Field<String>         COMMENTS_AUTHOR     = DSL.field(name("comments", "author"),     SQLDataType.VARCHAR(50).notNull());
    public static final Field<String>         COMMENTS_BODY       = DSL.field(name("comments", "body"),       SQLDataType.VARCHAR(500).notNull());
    public static final Field<OffsetDateTime> COMMENTS_CREATED_AT = DSL.field(name("comments", "created_at"), SQLDataType.TIMESTAMPWITHTIMEZONE.notNull());

    private static Name name(String table, String col) {
        return DSL.name(table, col);
    }
}
