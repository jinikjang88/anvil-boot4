package com.devsmith.anvil.ch08.config;

import com.devsmith.anvil.ch08.domain.Comment;
import com.devsmith.anvil.ch08.domain.Post;
import com.devsmith.anvil.ch08.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 부팅 시 게시글 + 댓글을 시드한다.
 * anvil.seed.enabled=true 일 때만 동작.
 *
 * N+1 시연을 위해 게시글마다 댓글이 여러 개 붙어 있어야 한다.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    @Transactional
    ApplicationRunner seedRunner(SeedProperties props, PostRepository postRepository) {
        return args -> {
            if (!props.enabled()) {
                log.info("[seed] disabled — anvil.seed.enabled=false");
                return;
            }
            if (postRepository.count() > 0) {
                log.info("[seed] skipped — posts already exist");
                return;
            }
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 1; i <= props.postCount(); i++) {
                Post post = new Post("게시글 #" + i, "본문 내용 " + i);
                int n = rnd.nextInt(props.minComments(), props.maxComments() + 1);
                for (int j = 1; j <= n; j++) {
                    post.addComment(new Comment("작성자" + j, "댓글 " + j + " on post " + i));
                }
                postRepository.save(post);
            }
            log.info("[seed] inserted {} posts", props.postCount());
        };
    }
}
