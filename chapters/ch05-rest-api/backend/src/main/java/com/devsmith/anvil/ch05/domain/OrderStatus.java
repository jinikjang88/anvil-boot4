package com.devsmith.anvil.ch05.domain;

/**
 * 주문 상태.
 *
 * <p>전이 규칙은 {@link OrderStateMachine} 이 단일 출처. enum 자체엔 비즈니스 규칙을 두지 않는다.
 * (서비스 / 도메인 규칙은 한 곳에 모이는 게 변경 추적에 유리.)</p>
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
