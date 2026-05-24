package com.devsmith.anvil.ch09.search;

import java.time.Instant;

/**
 * 게시글 검색 조건 — 모두 nullable.
 *
 * 학습 의도: 5개 조건이 각각 null 일 수 있는 25 = 32 가지 조합.
 * 메서드 이름으로 표현하려면 32개 메서드, JPQL 문자열로 짜면 if 문 폭발.
 * QueryDSL 은 BooleanExpression 이 null 이면 자동 무시 → 같은 코드로 모두 대응.
 *
 * @param keyword       제목/본문 부분 일치 (대소문자 무시)
 * @param author        작성자 정확 일치
 * @param from          createdAt &gt;= from
 * @param to            createdAt &lt; to
 * @param hasComments   true 면 댓글 있는 글만, false 면 댓글 없는 글만, null 이면 무시
 */
public record PostSearchCondition(
        String keyword,
        String author,
        Instant from,
        Instant to,
        Boolean hasComments
) {
    /** 빈 조건 — 전체 조회와 동일. 테스트 가독성을 위한 헬퍼. */
    public static PostSearchCondition empty() {
        return new PostSearchCondition(null, null, null, null, null);
    }
}
