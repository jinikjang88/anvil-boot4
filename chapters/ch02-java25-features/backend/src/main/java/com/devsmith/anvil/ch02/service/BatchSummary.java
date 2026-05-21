package com.devsmith.anvil.ch02.service;

import java.util.List;

/**
 * 배치 처리 결과 요약 — 응답 DTO.
 *
 * <p>중첩 record(Counts) 로 응답 구조를 명확히 한다.
 * record 안에 record 가 들어가도 컴파일러가 모든 직렬화/equals/hash 를 처리.</p>
 */
public record BatchSummary(
        String executor,        // "platform" | "virtual"
        int total,
        long elapsedMs,
        Counts counts,
        List<String> samples    // 처음 N 건의 사람-읽기용 설명 (UI 표시용)
) {
    public record Counts(int approved, int rejected, int pending) { }
}
