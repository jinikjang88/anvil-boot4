package com.devsmith.anvil.ch09b.search;

import java.time.OffsetDateTime;

/**
 * 검색 조건 — ch09 (QueryDSL) 의 PostSearchCondition 과 같은 모양.
 * 학습 의도: "같은 조건" 으로 두 챕터를 호출해 SQL 을 비교.
 *
 * 타입 한 가지가 다르다 — Instant → OffsetDateTime.
 *   jOOQ + JDBC 의 timestamptz 표준 매핑이 OffsetDateTime 이라 자연스러움.
 *   (Instant 도 가능하지만 변환이 한 번 더 든다.)
 */
public record PostSearchCondition(
        String keyword,
        String author,
        OffsetDateTime from,
        OffsetDateTime to,
        Boolean hasComments
) {
    public static PostSearchCondition empty() {
        return new PostSearchCondition(null, null, null, null, null);
    }
}
