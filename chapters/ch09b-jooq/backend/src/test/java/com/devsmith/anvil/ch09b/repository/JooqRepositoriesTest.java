package com.devsmith.anvil.ch09b.repository;

import com.devsmith.anvil.ch09b.PostgresContainerSpec;
import com.devsmith.anvil.ch09b.config.SqlCapture;
import com.devsmith.anvil.ch09b.repository.JooqBulkInsertRepository.CommentInput;
import com.devsmith.anvil.ch09b.repository.JooqCommentTreeRepository.CommentNode;
import com.devsmith.anvil.ch09b.repository.JooqRankingRepository.AuthorRanking;
import com.devsmith.anvil.ch09b.search.PostSearchCondition;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

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
import static org.assertj.core.api.Assertions.assertThat;

/**
 * jOOQ 5가지 기능 통합 검증.
 *
 * 같은 테스트 클래스에서 SchemaInitializer 가 만든 빈 테이블에 직접 데이터를 채우고
 * 각 리포지토리의 동작을 검증한다.
 */
@SpringBootTest
@DisplayName("ch09b — jOOQ 5가지 기능")
class JooqRepositoriesTest extends PostgresContainerSpec {

    @Autowired DSLContext dsl;
    @Autowired JooqPostSearchRepository search;
    @Autowired JooqRankingRepository ranking;
    @Autowired JooqCommentTreeRepository tree;
    @Autowired JooqUpsertRepository upsert;
    @Autowired JooqBulkInsertRepository bulk;
    @Autowired SqlCapture capture;

    private long p1, p2, p3;  // 게시글 id (트리 시연용 p2)

    @BeforeEach
    void seed() {
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(POSTS).execute();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        p1 = insertPost("jOOQ 입문",   "민지", now);
        p2 = insertPost("트리 댓글",   "민지", now);
        p3 = insertPost("랭킹 시연",   "준호", now.minus(1, ChronoUnit.DAYS));
        insertPost(  "Spring",       "준호", now.minus(2, ChronoUnit.DAYS));
        insertPost(  "수아의 글 1",   "수아", now.minus(3, ChronoUnit.DAYS));
        insertPost(  "수아의 글 2",   "수아", now.minus(4, ChronoUnit.DAYS));

        // p2 에 댓글 트리 (depth 3)
        long c1 = insertComment(p2, null, "anon", "root 1");
        long c2 = insertComment(p2, c1,   "민지", "child 1.1");
        insertComment(p2, c2, "anon", "child 1.1.1");
        insertComment(p2, null, "준호", "root 2");

        // p1 에 댓글 1 (검색 hasComments 시연)
        insertComment(p1, null, "anon", "한 줄 댓글");
    }

    private long insertPost(String title, String author, OffsetDateTime createdAt) {
        return dsl.insertInto(POSTS)
                .columns(POSTS_TITLE, POSTS_CONTENT, POSTS_AUTHOR, POSTS_CREATED_AT)
                .values(title, "본문 " + title, author, createdAt)
                .returning(POSTS_ID)
                .fetchOne()
                .get(POSTS_ID);
    }

    private long insertComment(long postId, Long parentId, String author, String body) {
        return dsl.insertInto(COMMENTS)
                .columns(COMMENTS_POST_ID, COMMENTS_PARENT_ID, COMMENTS_AUTHOR, COMMENTS_BODY)
                .values(postId, parentId, author, body)
                .returning(COMMENTS_ID)
                .fetchOne()
                .get(COMMENTS_ID);
    }

    // ───────────────────────────────────────────────────────────────────
    // 1) 검색
    // ───────────────────────────────────────────────────────────────────
    @Test
    void 검색_조건_모두_null_이면_전체_반환() {
        List<PostRow> all = search.search(PostSearchCondition.empty(), 0, 100);
        assertThat(all).hasSize(6);
        assertThat(search.count(PostSearchCondition.empty())).isEqualTo(6);
    }

    @Test
    void 검색_author_조건만_적용() {
        List<PostRow> rows = search.search(
                new PostSearchCondition(null, "민지", null, null, null), 0, 100);
        assertThat(rows).extracting(PostRow::author).containsOnly("민지");
        assertThat(rows).hasSize(2);
    }

    @Test
    void 검색_hasComments_true_는_exists_서브쿼리로_댓글_있는_글만() {
        List<PostRow> rows = search.search(
                new PostSearchCondition(null, null, null, null, true), 0, 100);
        // p1, p2 만 댓글이 있음
        assertThat(rows).extracting(PostRow::id).containsExactlyInAnyOrder(p1, p2);
    }

    // ───────────────────────────────────────────────────────────────────
    // 2) 윈도우 함수
    // ───────────────────────────────────────────────────────────────────
    @Test
    void 작성자별_랭킹_top1_은_각_작성자_1행만() {
        List<AuthorRanking> top1 = ranking.rankPostsByAuthor(1);
        // 작성자 3명 → 각 1행씩
        assertThat(top1).hasSize(3);
        assertThat(top1).extracting(AuthorRanking::rankInAuthor).containsOnly(1);
    }

    @Test
    void 작성자별_랭킹_top2_는_각_작성자_2행까지() {
        List<AuthorRanking> top2 = ranking.rankPostsByAuthor(2);
        // 민지 2 + 준호 2 + 수아 2 = 6
        assertThat(top2).hasSize(6);
        // 같은 author 안에서 rn 은 1,2,1,2,1,2 패턴
        assertThat(top2).extracting(AuthorRanking::rankInAuthor)
                .containsExactly(1, 2, 1, 2, 1, 2);
    }

    // ───────────────────────────────────────────────────────────────────
    // 3) WITH RECURSIVE
    // ───────────────────────────────────────────────────────────────────
    @Test
    void recursive_CTE_가_댓글_트리를_깊이_3까지_가져온다() {
        List<CommentNode> nodes = tree.tree(p2);

        // 총 4개 — root 2개 + child 1개 + grandchild 1개
        assertThat(nodes).hasSize(4);
        assertThat(nodes).extracting(CommentNode::depth)
                .containsExactlyInAnyOrder(1, 1, 2, 3);

        int maxDepth = nodes.stream().mapToInt(CommentNode::depth).max().orElse(0);
        assertThat(maxDepth).isEqualTo(3);

        // 발행 SQL 에 WITH RECURSIVE 가 포함되어야 함
        // (SqlCapture 가 startNanos 이전 호출이라 별도로 캡처해서 확인)
        capture.start();
        tree.tree(p2);
        List<String> sql = capture.drain();
        assertThat(sql).anyMatch(s -> s.toLowerCase().contains("with recursive"));
    }

    @Test
    void recursive_CTE_빈_게시글_은_빈_결과() {
        List<CommentNode> nodes = tree.tree(p3);
        assertThat(nodes).isEmpty();
    }

    // ───────────────────────────────────────────────────────────────────
    // 4) ON CONFLICT DO UPDATE
    // ───────────────────────────────────────────────────────────────────
    @Test
    void upsert_같은_title_author_재호출시_본문만_갱신() {
        long firstId  = upsert.upsert("upsert 시험", "본문 v1", "민지");
        long secondId = upsert.upsert("upsert 시험", "본문 v2", "민지");

        assertThat(secondId).isEqualTo(firstId);

        String content = dsl.select(POSTS_CONTENT).from(POSTS).where(POSTS_ID.eq(firstId))
                .fetchOne(POSTS_CONTENT);
        assertThat(content).isEqualTo("본문 v2");
    }

    @Test
    void upsert_다른_author_면_새_row() {
        long idA = upsert.upsert("같은 제목", "본문", "민지");
        long idB = upsert.upsert("같은 제목", "본문", "준호");

        assertThat(idB).isNotEqualTo(idA);
    }

    // ───────────────────────────────────────────────────────────────────
    // 5) Bulk INSERT
    // ───────────────────────────────────────────────────────────────────
    @Test
    void bulk_insert_는_한_SQL_에_N_rows() {
        List<CommentInput> hundred = IntStream.range(0, 100)
                .mapToObj(i -> new CommentInput("user" + i, "댓글 " + i))
                .toList();

        capture.start();
        int n = bulk.bulkInsert(p3, hundred);
        List<String> sql = capture.drain();

        assertThat(n).isEqualTo(100);
        // 발행 SQL 1개 (INSERT INTO ... VALUES (?,...),(?,...),...).
        long insertCount = sql.stream().filter(s -> s.toLowerCase().startsWith("insert")).count();
        assertThat(insertCount).as("진짜 batch — 단일 INSERT 한 방").isEqualTo(1);
    }
}
