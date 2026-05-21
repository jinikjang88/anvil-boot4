package com.devsmith.anvil.ch02.domain;

import java.math.BigDecimal;

/**
 * 주문 요청.
 *
 * <p>Java 25 <b>record</b> — 불변 데이터 캐리어. equals/hashCode/toString 자동 생성.
 * compact constructor 로 도메인 불변식(amount &gt; 0)을 생성 시점에 강제한다.</p>
 *
 * <p>학습 포인트: "DTO 클래스 60줄짜리" 시절의 보일러플레이트가 record 한 줄로 사라진다.</p>
 */
public record OrderRequest(String orderId, BigDecimal amount) {

    public OrderRequest {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
    }
}
