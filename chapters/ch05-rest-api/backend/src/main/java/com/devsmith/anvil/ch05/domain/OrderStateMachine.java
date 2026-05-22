package com.devsmith.anvil.ch05.domain;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 주문 상태 전이 규칙의 단일 출처.
 *
 * <pre>
 *   PENDING   → CONFIRMED, CANCELLED
 *   CONFIRMED → SHIPPED,   CANCELLED
 *   SHIPPED   → DELIVERED
 *   DELIVERED → (종료)
 *   CANCELLED → (종료)
 * </pre>
 *
 * <p>학습 포인트 — 비즈니스 규칙을 컨트롤러에 흩뿌리지 않는다.
 * 컨트롤러는 HTTP 변환만, 도메인 규칙은 도메인 계층의 한 클래스에 모은다 (ch03 의 SRP 원칙 연속).</p>
 */
@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PENDING,   Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPED,   OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.SHIPPED,   Set.of(OrderStatus.DELIVERED));
        ALLOWED.put(OrderStatus.DELIVERED, Set.of());   // 종료
        ALLOWED.put(OrderStatus.CANCELLED, Set.of());   // 종료
    }

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) return false;
        if (from == to) return false;                   // 자기 자신으로의 전이 차단 (no-op 으로 200 주는 함정 회피)
        return ALLOWED.get(from).contains(to);
    }

    public Set<OrderStatus> allowedFrom(OrderStatus from) {
        return ALLOWED.getOrDefault(from, Set.of());
    }
}
