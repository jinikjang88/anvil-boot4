package com.devsmith.anvil.ch08.controller;

import com.devsmith.anvil.ch08.config.SqlCapture;
import com.devsmith.anvil.ch08.controller.PostDtos.CreateCommentRequest;
import com.devsmith.anvil.ch08.controller.PostDtos.CreatePostRequest;
import com.devsmith.anvil.ch08.controller.PostDtos.PostResponse;
import com.devsmith.anvil.ch08.controller.PostDtos.PostsEnvelope;
import com.devsmith.anvil.ch08.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 게시글 REST API.
 *
 * - GET  /api/v1/posts/lazy  → N+1 발생 시연 (game over)
 * - GET  /api/v1/posts/fetch → @EntityGraph 해법 (winner)
 * - POST /api/v1/posts                  → 게시글 작성
 * - POST /api/v1/posts/{id}/comments    → 댓글 작성
 *
 * 응답 봉투에 SQL 로그를 동봉해 프론트가 좌우 비교하게 만든다.
 */
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService service;
    private final SqlCapture capture;

    public PostController(PostService service, SqlCapture capture) {
        this.service = service;
        this.capture = capture;
    }

    @GetMapping("/lazy")
    public PostsEnvelope lazy() {
        capture.start();
        long startNanos = System.nanoTime();
        List<PostResponse> data = service.listLazy();
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        return PostsEnvelope.of("LAZY (N+1)", data, capture.drain(), elapsedMs);
    }

    @GetMapping("/fetch")
    public PostsEnvelope fetch() {
        capture.start();
        long startNanos = System.nanoTime();
        List<PostResponse> data = service.listFetch();
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        return PostsEnvelope.of("FETCH JOIN (@EntityGraph)", data, capture.drain(), elapsedMs);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody CreatePostRequest req) {
        return service.create(req.title(), req.content());
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse addComment(@PathVariable Long id,
                                   @Valid @RequestBody CreateCommentRequest req) {
        return service.addComment(id, req.author(), req.body());
    }
}
