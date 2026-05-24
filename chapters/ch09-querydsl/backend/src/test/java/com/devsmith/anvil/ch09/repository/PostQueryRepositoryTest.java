package com.devsmith.anvil.ch09.repository;

import com.devsmith.anvil.ch09.PostgresContainerSpec;
import com.devsmith.anvil.ch09.config.SqlCapture;
import com.devsmith.anvil.ch09.domain.Comment;
import com.devsmith.anvil.ch09.domain.Post;
import com.devsmith.anvil.ch09.search.PostSearchCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QueryDSL 동적 검색의 행동을 확인.
 *
 * 검증 포인트:
 *  - 조건 0개 → 전체 조회 (조건 5개가 모두 null 일 때 where 가 비어야 함)
 *  - 조건 1개씩 → 해당 조건만 SQL 에 추가됨
 *  - hasComments=true → exists 서브쿼리 발행 + 댓글 없는 글 제외
 *  - BooleanBuilder / BooleanExpression 결과 동일성 (다른 스타일, 같은 결과)
 */
@SpringBootTest
@DisplayName("PostQueryRepository — 동적 검색")
class PostQueryRepositoryTest extends PostgresContainerSpec {

    @Autowired PostRepository repository;
    @Autowired SqlCapture capture;

    private Instant now;
    private Instant yesterday;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        now = Instant.now();
        yesterday = now.minus(1, ChronoUnit.DAYS);

        save("QueryDSL 입문",   "타입 세이프 쿼리",     "민지", now,       true);
        save("ORM 의 한계",     "복잡 집계는 어렵다",   "준호", now,       false);
        save("Spring Boot 4",   "Java 25 토대",        "준호", yesterday, true);
        save("JPA N+1",         "ch08 의 함정",        "민지", yesterday, false);
    }

    private void save(String title, String content, String author, Instant createdAt, boolean withComments) {
        Post p = new Post(title, content, author, createdAt);
        if (withComments) {
            p.addComment(new Comment("anon", "좋아요"));
        }
        repository.save(p);
    }

    @Test
    @Transactional
    void 조건이_모두_null_이면_전체를_반환한다() {
        Page<Post> page = repository.searchByBooleanExpression(
                PostSearchCondition.empty(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent()).hasSize(4);
    }

    @Test
    @Transactional
    void author_조건만_지정하면_해당_작성자만_반환한다() {
        capture.start();

        Page<Post> page = repository.searchByBooleanExpression(
                new PostSearchCondition(null, "민지", null, null, null),
                PageRequest.of(0, 10));

        List<String> sql = capture.drain();
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Post::getAuthor).containsOnly("민지");
        // author 조건은 SQL 에 등장, 다른 조건은 등장하지 않음.
        String selectSql = sql.stream().filter(s -> s.toLowerCase().contains("from posts")).findFirst().orElseThrow();
        assertThat(selectSql.toLowerCase()).contains("author");
        assertThat(selectSql.toLowerCase()).doesNotContain("lower(");
    }

    @Test
    @Transactional
    void keyword_조건은_제목과_본문에_OR_로_적용된다() {
        Page<Post> page = repository.searchByBooleanExpression(
                new PostSearchCondition("QueryDSL", null, null, null, null),
                PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("QueryDSL 입문");
    }

    @Test
    @Transactional
    void hasComments_true_면_댓글이_있는_글만() {
        Page<Post> page = repository.searchByBooleanExpression(
                new PostSearchCondition(null, null, null, null, true),
                PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Post::getTitle)
                .containsExactlyInAnyOrder("QueryDSL 입문", "Spring Boot 4");
    }

    @Test
    @Transactional
    void hasComments_false_면_댓글이_없는_글만() {
        Page<Post> page = repository.searchByBooleanExpression(
                new PostSearchCondition(null, null, null, null, false),
                PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Post::getTitle)
                .containsExactlyInAnyOrder("ORM 의 한계", "JPA N+1");
    }

    @Test
    @Transactional
    void from_조건은_경계값을_포함한다() {
        Page<Post> page = repository.searchByBooleanExpression(
                new PostSearchCondition(null, null, now, null, null),
                PageRequest.of(0, 10));

        // now 보다 같거나 큰 글만 (오늘 작성한 2건).
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @Transactional
    void BooleanBuilder_와_BooleanExpression_은_동일한_결과를_낸다() {
        PostSearchCondition cond = new PostSearchCondition("Spring", null, null, null, true);

        Page<Post> byBuilder    = repository.searchByBooleanBuilder(cond, PageRequest.of(0, 10));
        Page<Post> byExpression = repository.searchByBooleanExpression(cond, PageRequest.of(0, 10));

        assertThat(byBuilder.getTotalElements()).isEqualTo(byExpression.getTotalElements());
        assertThat(byBuilder.getContent()).extracting(Post::getId)
                .isEqualTo(byExpression.getContent().stream().map(Post::getId).toList());
    }

    @Test
    @Transactional
    void JPQL_concat_도_같은_결과를_낸다() {
        // 안티패턴 시연용 메서드도 결과 자체는 동일해야 함.
        PostSearchCondition cond = new PostSearchCondition("Spring", null, null, null, true);

        List<Post> jpql = repository.searchByJpqlConcat(cond);
        Page<Post> ql  = repository.searchByBooleanExpression(cond, PageRequest.of(0, 10));

        assertThat(jpql).extracting(Post::getId)
                .containsExactlyInAnyOrderElementsOf(ql.getContent().stream().map(Post::getId).toList());
    }
}
