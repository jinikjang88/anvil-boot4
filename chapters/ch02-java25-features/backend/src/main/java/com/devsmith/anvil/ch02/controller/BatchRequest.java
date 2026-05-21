package com.devsmith.anvil.ch02.controller;

import java.math.BigDecimal;

/**
 * 배치 처리 요청 — 동시 요청 수와 금액 범위.
 *
 * <p>record compact constructor 로 입력 검증을 한 곳에 모았다.
 * (ch06-validation 에서 Bean Validation 으로 더 정교한 방식을 다루지만,
 * ch02 단계에서는 record 의 표현력만으로 충분.)</p>
 */
public record BatchRequest(int count, BigDecimal amountMin, BigDecimal amountMax) {

    /** UI 가 너무 큰 값을 보내 워커가 죽는 일을 막기 위한 상한선. */
    public static final int MAX_COUNT = 500;

    public BatchRequest {
        if (count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("count must be 1.." + MAX_COUNT + " (got " + count + ")");
        }
        if (amountMin == null) {
            amountMin = new BigDecimal("1000");
        }
        if (amountMax == null) {
            amountMax = new BigDecimal("50000");
        }
        if (amountMin.compareTo(amountMax) > 0) {
            throw new IllegalArgumentException("amountMin > amountMax");
        }
    }
}
