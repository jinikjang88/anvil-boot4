package com.devsmith.anvil.ch05.domain;

import java.math.BigDecimal;

/**
 * 주문 라인 아이템. record + compact constructor 로 도메인 불변식을 생성 시점에 강제.
 */
public record OrderItem(String sku, String name, int quantity, BigDecimal price) {

    public OrderItem {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1 (got " + quantity + ")");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("price must be >= 0");
        }
    }

    public BigDecimal subtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
