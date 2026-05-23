package com.devsmith.anvil.ch08.repository;

import com.devsmith.anvil.ch08.PostgresContainerSpec;
import com.devsmith.anvil.ch08.config.SqlCapture;
import com.devsmith.anvil.ch08.domain.Comment;
import com.devsmith.anvil.ch08.domain.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * N+1 의 실재 여부를 SQL 로그로 검증.
 * - 단순 findAll() + LAZY 접근 → select 쿼리 6+ (게시글 1 + 댓글 N)
 * - findAllWithComments() (@EntityGraph) → select 1~2
 */
@SpringBootTest
@DisplayName("PostRepository — N+1 시연")
class PostRepositoryNPlusOneTest extends PostgresContainerSpec {

    @Autowired PostRepository postRepository;
    @Autowired SqlCapture capture;

    @BeforeEach
    void seed() {
        postRepository.deleteAll();
        for (int i = 1; i <= 5; i++) {
            Post p = new Post("제목 " + i, "본문 " + i);
            for (int j = 1; j <= 3; j++) {
                p.addComment(new Comment("작성자" + j, "댓글 " + j));
            }
            postRepository.save(p);
        }
    }

    @Test
    @Transactional
    void findAll_후_LAZY_접근하면_N_플러스_1_쿼리가_발생한다() {
        capture.start();

        List<Post> posts = postRepository.findAll();
        long total = posts.stream().mapToLong(p -> p.getComments().size()).sum();

        List<String> sql = capture.drain();
        long selectCount = sql.stream().filter(s -> s.toLowerCase().startsWith("select")).count();

        assertThat(posts).hasSize(5);
        assertThat(total).isEqualTo(15);
        // 게시글 1 + 댓글 5 = 최소 6 select.
        assertThat(selectCount).as("N+1 발생 시 select 6+ 예상").isGreaterThanOrEqualTo(6);
    }

    @Test
    @Transactional
    void EntityGraph_적용하면_단일_쿼리로_가져온다() {
        capture.start();

        List<Post> posts = postRepository.findAllWithComments();
        long total = posts.stream().mapToLong(p -> p.getComments().size()).sum();

        List<String> sql = capture.drain();
        long selectCount = sql.stream().filter(s -> s.toLowerCase().startsWith("select")).count();

        assertThat(posts).hasSize(5);
        assertThat(total).isEqualTo(15);
        // join 한 번으로 끝.
        assertThat(selectCount).as("@EntityGraph 시 select 1 예상").isEqualTo(1);
    }
}
