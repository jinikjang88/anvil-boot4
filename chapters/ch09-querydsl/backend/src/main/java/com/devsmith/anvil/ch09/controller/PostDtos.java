package com.devsmith.anvil.ch09.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * 검색 챕터 DTO — Entity 노출 금지 원칙은 ch08 과 동일.
 */
public final class PostDtos {

    private PostDtos() {}

    public record CreatePostRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            @NotBlank @Size(max = 50) String author
    ) {}

    public record CreateCommentRequest(
            @NotBlank @Size(max = 50) String author,
            @NotBlank @Size(max = 500) String body
    ) {}

    public record CommentSummary(Long id, String author) {}

    public record PostResponse(
            Long id,
            String title,
            String content,
            String author,
            Instant createdAt,
            int commentCount,
            List<CommentSummary> comments
    ) {}

    /**
     * 검색 응답 봉투 — 데이터 + 페이지 메타 + 발행 SQL.
     * 프론트는 jpql / builder / expression 세 호출의 sqlLogs 를 사이드바이사이드로 비교.
     */
    public record SearchEnvelope(
            String mode,
            List<PostResponse> data,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<String> sqlLogs,
            int sqlCount,
            long elapsedMillis
    ) {
        public static SearchEnvelope of(String mode,
                                        List<PostResponse> data,
                                        int page, int size,
                                        long totalElements, int totalPages,
                                        List<String> sqlLogs,
                                        long elapsedMillis) {
            return new SearchEnvelope(mode, data, page, size, totalElements, totalPages,
                    sqlLogs, sqlLogs.size(), elapsedMillis);
        }
    }
}
