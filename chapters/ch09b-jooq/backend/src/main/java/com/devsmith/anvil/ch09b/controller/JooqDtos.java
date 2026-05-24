package com.devsmith.anvil.ch09b.controller;

import com.devsmith.anvil.ch09b.repository.JooqCommentTreeRepository.CommentNode;
import com.devsmith.anvil.ch09b.repository.JooqRankingRepository.AuthorRanking;
import com.devsmith.anvil.ch09b.repository.PostRow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** ch09b API DTO. 응답 봉투는 모드별로 다른 데이터를 담고 SQL 로그를 공통으로 첨부. */
public final class JooqDtos {

    private JooqDtos() {}

    // ── 요청 ─────────────────────────────────────────────────────────────
    public record UpsertRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            @NotBlank @Size(max = 50) String author
    ) {}

    public record BulkCommentRequest(
            long postId,
            List<CommentItem> comments
    ) {
        public record CommentItem(
                @NotBlank @Size(max = 50) String author,
                @NotBlank @Size(max = 500) String body
        ) {}
    }

    // ── 응답 봉투 ────────────────────────────────────────────────────────
    public record SearchEnvelope(
            String mode,
            List<PostRow> data,
            int page,
            int size,
            int totalElements,
            List<String> sqlLogs,
            int sqlCount,
            long elapsedMillis
    ) {}

    public record RankingEnvelope(
            String mode,
            List<AuthorRanking> data,
            int topNPerAuthor,
            List<String> sqlLogs,
            int sqlCount,
            long elapsedMillis
    ) {}

    public record TreeEnvelope(
            String mode,
            long postId,
            List<CommentNode> data,
            int totalNodes,
            int maxDepth,
            List<String> sqlLogs,
            int sqlCount,
            long elapsedMillis
    ) {}

    public record UpsertEnvelope(
            String mode,
            long id,
            String operation,        // "INSERT" 추정 / "UPDATE" 추정 — 사실 jOOQ 는 구분 안 되지만 의미용 라벨
            List<String> sqlLogs,
            int sqlCount,
            long elapsedMillis
    ) {}

    public record BulkInsertEnvelope(
            String mode,
            long postId,
            int rowsInserted,
            List<String> sqlLogs,
            int sqlCount,
            long elapsedMillis
    ) {}
}
