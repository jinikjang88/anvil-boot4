package com.devsmith.anvil.ch05.service;

import com.devsmith.anvil.ch05.domain.Order;
import com.devsmith.anvil.ch05.domain.OrderItem;
import com.devsmith.anvil.ch05.domain.OrderStateMachine;
import com.devsmith.anvil.ch05.domain.OrderStatus;
import com.devsmith.anvil.ch05.error.IllegalTransitionException;
import com.devsmith.anvil.ch05.error.NotFoundException;
import com.devsmith.anvil.ch05.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 주문 도메인 오케스트레이션.
 *
 * <p>컨트롤러는 HTTP 변환만, 도메인 규칙(상태 전이, 총액 계산)은 모두 여기로 모은다.
 * 단위 테스트가 가능한 형태 (ch03 의 DI 원칙 연속) — Clock 까지 주입받아 시간 결정적.</p>
 */
@Service
public class OrderService {

    private final OrderRepository repository;
    private final OrderStateMachine stateMachine;
    private final Clock clock;

    public OrderService(OrderRepository repository, OrderStateMachine stateMachine, Clock clock) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.clock = clock;
    }

    public Order create(String customer, List<OrderItem> items, String memo) {
        Instant now = Instant.now(clock);
        Order order = new Order(
                UUID.randomUUID().toString(),
                customer,
                items,
                Order.totalOf(items),
                OrderStatus.PENDING,    // 신규 주문은 항상 PENDING 부터
                memo,
                now,
                now
        );
        return repository.save(order);
    }

    public OrderRepository.Page<Order> list(OrderStatus statusFilter, int page, int size) {
        if (page < 0)  throw new IllegalArgumentException("page must be >= 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be 1..100");
        return repository.findAll(statusFilter, page, size);
    }

    public Order get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("order not found: " + id));
    }

    /** PATCH — 부분 수정. 본 챕터에선 memo 만 수정 가능 (단순화). */
    public Order updateMemo(String id, String newMemo) {
        Order current = get(id);
        Order updated = current.withMemo(newMemo == null ? "" : newMemo, Instant.now(clock));
        return repository.save(updated);
    }

    /** PUT /{id}/status — 단일 필드의 전체 교체이자 상태 전이. */
    public Order transition(String id, OrderStatus target) {
        Order current = get(id);
        if (!stateMachine.canTransition(current.status(), target)) {
            throw new IllegalTransitionException(current.status(), target);
        }
        Order updated = current.withStatus(target, Instant.now(clock));
        return repository.save(updated);
    }

    public void delete(String id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException("order not found: " + id);
        }
    }
}
