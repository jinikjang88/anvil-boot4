package com.devsmith.anvil.ch09b.controller;

import com.devsmith.anvil.ch09b.config.SqlCapture;
import com.devsmith.anvil.ch09b.controller.JooqDtos.BulkCommentRequest;
import com.devsmith.anvil.ch09b.controller.JooqDtos.BulkInsertEnvelope;
import com.devsmith.anvil.ch09b.controller.JooqDtos.RankingEnvelope;
import com.devsmith.anvil.ch09b.controller.JooqDtos.SearchEnvelope;
import com.devsmith.anvil.ch09b.controller.JooqDtos.TreeEnvelope;
import com.devsmith.anvil.ch09b.controller.JooqDtos.UpsertEnvelope;
import com.devsmith.anvil.ch09b.controller.JooqDtos.UpsertRequest;
import com.devsmith.anvil.ch09b.repository.JooqBulkInsertRepository;
import com.devsmith.anvil.ch09b.repository.JooqBulkInsertRepository.CommentInput;
import com.devsmith.anvil.ch09b.repository.JooqCommentTreeRepository;
import com.devsmith.anvil.ch09b.repository.JooqCommentTreeRepository.CommentNode;
import com.devsmith.anvil.ch09b.repository.JooqPostSearchRepository;
import com.devsmith.anvil.ch09b.repository.JooqRankingRepository;
import com.devsmith.anvil.ch09b.repository.JooqRankingRepository.AuthorRanking;
import com.devsmith.anvil.ch09b.repository.JooqUpsertRepository;
import com.devsmith.anvil.ch09b.repository.PostRow;
import com.devsmith.anvil.ch09b.search.PostSearchCondition;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * ch09b API — jOOQ 의 다섯 가지 시연.
 *
 *  GET  /api/v1/jooq/search                          : 동적 검색 (ch09 와 비교군)
 *  GET  /api/v1/jooq/ranking?topN=N                  : 윈도우 함수 ROW_NUMBER
 *  GET  /api/v1/jooq/tree/{postId}                   : WITH RECURSIVE 댓글 트리
 *  POST /api/v1/jooq/upsert                          : ON CONFLICT DO UPDATE
 *  POST /api/v1/jooq/bulk-comments                   : 진짜 batch INSERT
 *
 * 모든 응답에 sqlLogs / sqlCount / elapsedMillis 동봉 → 프론트가 발행 SQL 을 시각화.
 */
@RestController
@RequestMapping("/api/v1/jooq")
public class JooqController {

    private final JooqPostSearchRepository search;
    private final JooqRankingRepository ranking;
    private final JooqCommentTreeRepository tree;
    private final JooqUpsertRepository upsert;
    private final JooqBulkInsertRepository bulk;
    private final SqlCapture capture;

    public JooqController(JooqPostSearchRepository search,
                          JooqRankingRepository ranking,
                          JooqCommentTreeRepository tree,
                          JooqUpsertRepository upsert,
                          JooqBulkInsertRepository bulk,
                          SqlCapture capture) {
        this.search = search;
        this.ranking = ranking;
        this.tree = tree;
        this.upsert = upsert;
        this.bulk = bulk;
        this.capture = capture;
    }

    // ── 1) 검색 ─────────────────────────────────────────────────────────
    @GetMapping("/search")
    public SearchEnvelope search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Boolean hasComments,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        capture.start();
        long startNanos = System.nanoTime();
        PostSearchCondition cond = new PostSearchCondition(keyword, author, from, to, hasComments);
        List<PostRow> data = search.search(cond, page, size);
        int total = search.count(cond);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        List<String> sql = capture.drain();
        return new SearchEnvelope("jOOQ Condition (DSL.and)", data, page, size, total, sql, sql.size(), elapsedMs);
    }

    // ── 2) 윈도우 함수 랭킹 ─────────────────────────────────────────────
    @GetMapping("/ranking")
    public RankingEnvelope ranking(@RequestParam(defaultValue = "2") int topN) {
        capture.start();
        long startNanos = System.nanoTime();
        List<AuthorRanking> data = ranking.rankPostsByAuthor(topN);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        List<String> sql = capture.drain();
        return new RankingEnvelope("ROW_NUMBER() OVER (PARTITION BY author ORDER BY created_at DESC)",
                data, topN, sql, sql.size(), elapsedMs);
    }

    // ── 3) WITH RECURSIVE 트리 ──────────────────────────────────────────
    @GetMapping("/tree/{postId}")
    public TreeEnvelope tree(@PathVariable long postId) {
        capture.start();
        long startNanos = System.nanoTime();
        List<CommentNode> data = tree.tree(postId);
        int maxDepth = data.stream().mapToInt(CommentNode::depth).max().orElse(0);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        List<String> sql = capture.drain();
        return new TreeEnvelope("WITH RECURSIVE tree", postId, data, data.size(), maxDepth,
                sql, sql.size(), elapsedMs);
    }

    // ── 4) ON CONFLICT DO UPDATE ────────────────────────────────────────
    @PostMapping("/upsert")
    public UpsertEnvelope upsert(@Valid @RequestBody UpsertRequest req) {
        capture.start();
        long startNanos = System.nanoTime();
        long id = upsert.upsert(req.title(), req.content(), req.author());
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        List<String> sql = capture.drain();
        return new UpsertEnvelope("INSERT … ON CONFLICT (title, author) DO UPDATE",
                id, "upsert", sql, sql.size(), elapsedMs);
    }

    // ── 5) 진짜 batch INSERT ───────────────────────────────────────────
    @PostMapping("/bulk-comments")
    public BulkInsertEnvelope bulkComments(@Valid @RequestBody BulkCommentRequest req) {
        capture.start();
        long startNanos = System.nanoTime();
        List<CommentInput> inputs = req.comments().stream()
                .map(c -> new CommentInput(c.author(), c.body()))
                .toList();
        int n = bulk.bulkInsert(req.postId(), inputs);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        List<String> sql = capture.drain();
        return new BulkInsertEnvelope("INSERT … VALUES (?,?,?,?), … (한 SQL N rows)",
                req.postId(), n, sql, sql.size(), elapsedMs);
    }
}
