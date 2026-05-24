package com.devsmith.anvil.ch09b.repository;

import java.time.OffsetDateTime;

/** posts 테이블의 한 행 — jOOQ Record 와 분리된 도메인용 record. */
public record PostRow(
        Long id,
        String title,
        String content,
        String author,
        OffsetDateTime createdAt
) {}
