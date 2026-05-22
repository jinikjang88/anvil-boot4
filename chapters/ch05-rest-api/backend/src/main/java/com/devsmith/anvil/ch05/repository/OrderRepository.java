package com.devsmith.anvil.ch05.repository;

import com.devsmith.anvil.ch05.domain.Order;
import com.devsmith.anvil.ch05.domain.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * In-memory 저장소.
 *
 * <p>본 챕터의 학습 포인트는 REST 자체 — 영속 계층은 ch08 (JPA) 에서 본격 다룬다.
 * 그래서 가장 단순한 ConcurrentHashMap 기반.</p>
 */
@Repository
public class OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public Order save(Order order) {
        store.put(order.id(), order);
        return order;
    }

    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }

    /**
     * 단순 페이지네이션 + 상태 필터. 정렬은 createdAt desc 고정 — 학습 단순화.
     */
    public Page<Order> findAll(OrderStatus statusFilter, int page, int size) {
        Predicate<Order> filter = statusFilter == null
                ? order -> true
                : order -> order.status() == statusFilter;

        List<Order> matched = store.values().stream()
                .filter(filter)
                .sorted(Comparator.comparing(Order::createdAt).reversed())
                .toList();

        long total = matched.size();
        int from = Math.min(page * size, matched.size());
        int to   = Math.min(from + size, matched.size());

        return new Page<>(matched.subList(from, to), page, size, total);
    }

    public long count() {
        return store.size();
    }

    public record Page<T>(List<T> items, int page, int size, long totalElements) { }
}
