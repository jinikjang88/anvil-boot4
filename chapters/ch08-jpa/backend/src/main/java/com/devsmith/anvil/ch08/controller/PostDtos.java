package com.devsmith.anvil.ch08.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * 게시글 API DTO 들. JPA Entity 를 응답에 노출하지 않기 위한 record DTO.
 * - Entity 는 영속성 컨텍스트와 묶인 가변 객체.
 * - record 는 불변 + 자동 직렬화 → 응답 안전.
 */
public final class PostDtos {

    private PostDtos() {}

    public record CreatePostRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content
    ) {}

    public record CreateCommentRequest(
            @NotBlank @Size(max = 50) String author,
            @NotBlank @Size(max = 500) String body
    ) {}

    public record CommentResponse(
            Long id,
            String author,
            String body,
            Instant createdAt
    ) {}

    public record PostResponse(
            Long id,
            String title,
            String content,
            Instant createdAt,
            List<CommentResponse> comments
    ) {
        public int commentCount() { return comments.size(); }
    }

    /**
     * 목록 응답 봉투 — 데이터 + 발행된 SQL 로그 + 소요 시간.
     * 프론트는 LAZY 호출과 FETCH 호출의 sqlLogs.length / elapsedMillis 를 사이드바이사이드 비교.
     */
    public record PostsEnvelope(
            String mode,
            List<PostResponse> data,
            List<String> sqlLogs,
            int sqlCount,
            long elapsedMillis
    ) {
        public static PostsEnvelope of(String mode, List<PostResponse> data,
                                       List<String> sqlLogs, long elapsedMillis) {
            return new PostsEnvelope(mode, data, sqlLogs, sqlLogs.size(), elapsedMillis);
        }
    }
}
