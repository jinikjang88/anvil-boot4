package com.devsmith.anvil.ch06.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** 검증을 통과한 후 저장되는 도메인 객체 — 입력 DTO 와는 분리. */
public record Transfer(
        String id,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String currency,
        String memo,
        boolean urgent,
        String notifyEmail,
        LocalDate scheduledAt,
        Instant createdAt
) { }
