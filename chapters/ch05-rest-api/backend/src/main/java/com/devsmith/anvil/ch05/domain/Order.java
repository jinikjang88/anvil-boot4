package com.devsmith.anvil.ch05.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 주문 리소스 — REST 가 노출하는 단위.
 *
 * <p>도메인 record 를 그대로 응답 DTO 로 노출한다 (학습용 단순화).
 * 실무에선 도메인↔DTO 분리가 유리한 경우가 많다 (ch08 JPA 에서 다시 다룸).</p>
 */
public record Order(
        String id,
        String customer,
        List<OrderItem> items,
        BigDecimal totalAmount,
        OrderStatus status,
        String memo,
        Instant createdAt,
        Instant updatedAt
) {

    public Order {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (customer == null || customer.isBlank()) throw new IllegalArgumentException("customer required");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("at least one item required");
        if (status == null) throw new IllegalArgumentException("status required");
        // items 는 불변 리스트로 강제 — 호출자가 외부에서 추가/변경 못 하게
        items = List.copyOf(items);
        if (memo == null) memo = "";
    }

    public Order withStatus(OrderStatus newStatus, Instant now) {
        return new Order(id, customer, items, totalAmount, newStatus, memo, createdAt, now);
    }

    public Order withMemo(String newMemo, Instant now) {
        return new Order(id, customer, items, totalAmount, status, newMemo, createdAt, now);
    }

    public static BigDecimal totalOf(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
