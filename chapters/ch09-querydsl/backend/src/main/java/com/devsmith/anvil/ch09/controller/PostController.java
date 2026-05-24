package com.devsmith.anvil.ch09.controller;

import com.devsmith.anvil.ch09.config.SqlCapture;
import com.devsmith.anvil.ch09.controller.PostDtos.CreateCommentRequest;
import com.devsmith.anvil.ch09.controller.PostDtos.CreatePostRequest;
import com.devsmith.anvil.ch09.controller.PostDtos.PostResponse;
import com.devsmith.anvil.ch09.controller.PostDtos.SearchEnvelope;
import com.devsmith.anvil.ch09.search.PostSearchCondition;
import com.devsmith.anvil.ch09.service.PostSearchService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 검색 챕터 컨트롤러.
 *
 * - GET /api/v1/posts/search/jpql       → JPQL String concat (안티패턴 시연)
 * - GET /api/v1/posts/search/builder    → QueryDSL BooleanBuilder
 * - GET /api/v1/posts/search/expression → QueryDSL BooleanExpression (권장)
 * - POST /api/v1/posts                  → 게시글 작성
 * - POST /api/v1/posts/{id}/comments    → 댓글 작성
 *
 * 응답 봉투에 SQL 로그 동봉 → 프론트가 세 방식의 SQL 을 사이드바이사이드로 비교.
 *
 * Pageable 을 직접 받지 않고 page/size 만 받는 이유:
 *   학습용 — Spring Data Pageable argument resolver 마법 없이 명시적으로.
 */
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostSearchService service;
    private final SqlCapture capture;

    public PostController(PostSearchService service, SqlCapture capture) {
        this.service = service;
        this.capture = capture;
    }

    @GetMapping("/search/jpql")
    public SearchEnvelope searchJpql(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Boolean hasComments
    ) {
        capture.start();
        long startNanos = System.nanoTime();
        List<PostResponse> data = service.searchByJpql(
                new PostSearchCondition(keyword, author, from, to, hasComments));
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        // jpql 경로는 페이지 메타가 없으므로 0/0 으로 채움.
        return SearchEnvelope.of("JPQL String concat (anti-pattern)",
                data, 0, data.size(), data.size(), 1, capture.drain(), elapsedMs);
    }

    @GetMapping("/search/builder")
    public SearchEnvelope searchBuilder(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Boolean hasComments,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        capture.start();
        long startNanos = System.nanoTime();
        Page<PostResponse> result = service.searchByBuilder(
                new PostSearchCondition(keyword, author, from, to, hasComments),
                PageRequest.of(page, size));
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        return SearchEnvelope.of("QueryDSL BooleanBuilder",
                result.getContent(), page, size, result.getTotalElements(), result.getTotalPages(),
                capture.drain(), elapsedMs);
    }

    @GetMapping("/search/expression")
    public SearchEnvelope searchExpression(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Boolean hasComments,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        capture.start();
        long startNanos = System.nanoTime();
        Page<PostResponse> result = service.searchByExpression(
                new PostSearchCondition(keyword, author, from, to, hasComments),
                PageRequest.of(page, size));
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        return SearchEnvelope.of("QueryDSL BooleanExpression (recommended)",
                result.getContent(), page, size, result.getTotalElements(), result.getTotalPages(),
                capture.drain(), elapsedMs);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody CreatePostRequest req) {
        return service.create(req.title(), req.content(), req.author());
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse addComment(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest req) {
        return service.addComment(id, req.author(), req.body());
    }
}
