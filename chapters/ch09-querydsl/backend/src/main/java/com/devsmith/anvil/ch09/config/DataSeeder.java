package com.devsmith.anvil.ch09.config;

import com.devsmith.anvil.ch09.domain.Comment;
import com.devsmith.anvil.ch09.domain.Post;
import com.devsmith.anvil.ch09.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 학습용 시드 — 작성자/시점/댓글유무가 골고루 섞이게 12개 게시글 + 댓글.
 *
 * 의도:
 *  - "민지" 가 쓴 글 / "준호" 가 쓴 글 → author 필터 시연
 *  - 오늘 / 어제 / 1주 전 → from~to 필터 시연
 *  - 댓글 있는 글 / 없는 글 → hasComments 필터 시연
 *  - keyword "QueryDSL" 이 있는 글 / 없는 글 → keyword 필터 시연
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    ApplicationRunner seed(SeedProperties props, PostRepository repository) {
        return args -> {
            if (!props.enabled() || repository.count() > 0) {
                return;
            }
            doSeed(repository);
        };
    }

    @Transactional
    void doSeed(PostRepository repository) {
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant lastWeek = now.minus(7, ChronoUnit.DAYS);

        List<Post> posts = List.of(
                post("QueryDSL 시작하기", "타입 세이프 동적 쿼리 입문",      "민지", now,       true),
                post("QueryDSL 함정",     "BooleanExpression 의 null 무시",  "민지", now,       true),
                post("ORM 의 한계",       "복잡 집계는 어렵다",              "준호", now,       false),
                post("Spring Boot 4",     "Java 25 토대 위에서",            "준호", yesterday, true),
                post("JPA N+1",           "ch08 에서 다룬 함정",            "민지", yesterday, false),
                post("jOOQ 비교",         "QueryDSL vs jOOQ 패러다임 차이",  "지훈", yesterday, true),
                post("페이징 최적화",     "count 쿼리 생략 조건",            "지훈", yesterday, false),
                post("동적 정렬",         "Sort 화이트리스트 매핑",          "민지", lastWeek,  true),
                post("BooleanBuilder",    "명령형 스타일 단점",              "지훈", lastWeek,  false),
                post("타입 세이프",       "컴파일 타임 검증의 가치",          "준호", lastWeek,  true),
                post("프로젝션",          "Tuple / Projections.constructor", "민지", lastWeek,  false),
                post("Q클래스",           "annotationProcessor 설정",        "준호", lastWeek,  true)
        );

        repository.saveAll(posts);
        log.info("[ch09 seed] {} 게시글 시드 완료", posts.size());
    }

    private Post post(String title, String content, String author, Instant createdAt, boolean withComments) {
        Post p = new Post(title, content, author, createdAt);
        if (withComments) {
            p.addComment(new Comment("anon", "좋은 글 감사합니다"));
            p.addComment(new Comment("dev",  "BooleanExpression 패턴 좋네요"));
        }
        return p;
    }
}
