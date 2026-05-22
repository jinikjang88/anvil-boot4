package com.devsmith.anvil.ch05.service;

import com.devsmith.anvil.ch05.domain.Order;
import com.devsmith.anvil.ch05.domain.OrderItem;
import com.devsmith.anvil.ch05.domain.OrderStateMachine;
import com.devsmith.anvil.ch05.domain.OrderStatus;
import com.devsmith.anvil.ch05.error.IllegalTransitionException;
import com.devsmith.anvil.ch05.error.NotFoundException;
import com.devsmith.anvil.ch05.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ch03 의 패턴 — @SpringBootTest 없이 POJO 조립. */
@DisplayName("OrderService — 컨텍스트 없이 검증")
class OrderServiceTest {

    private OrderService service;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-05-21T00:00:00Z"), ZoneOffset.UTC);
        service = new OrderService(new OrderRepository(), new OrderStateMachine(), fixedClock);
    }

    @Test
    void 생성된_주문은_PENDING_상태이고_총액이_계산된다() {
        Order order = service.create("alice", List.of(
                new OrderItem("sku-1", "도토리", 3, new BigDecimal("1000")),
                new OrderItem("sku-2", "솔잎",   2, new BigDecimal("500"))
        ), null);

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.totalAmount()).isEqualByComparingTo("4000");
        assertThat(order.createdAt()).isEqualTo(Instant.parse("2026-05-21T00:00:00Z"));
    }

    @Test
    void 없는_id는_NotFound() {
        assertThatThrownBy(() -> service.get("nope"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void PATCH_memo는_상태와_총액을_바꾸지_않는다() {
        Order created = service.create("alice", List.of(item()), "before");

        Order patched = service.updateMemo(created.id(), "after");

        assertThat(patched.memo()).isEqualTo("after");
        assertThat(patched.status()).isEqualTo(created.status());
        assertThat(patched.totalAmount()).isEqualByComparingTo(created.totalAmount());
    }

    @Test
    void PENDING에서_CONFIRMED로의_PUT_status는_성공() {
        Order created = service.create("alice", List.of(item()), null);

        Order confirmed = service.transition(created.id(), OrderStatus.CONFIRMED);

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void PENDING에서_DELIVERED로의_PUT_status는_IllegalTransition() {
        Order created = service.create("alice", List.of(item()), null);

        assertThatThrownBy(() -> service.transition(created.id(), OrderStatus.DELIVERED))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void DELETE는_삭제_후_NotFound가_된다() {
        Order created = service.create("alice", List.of(item()), null);

        service.delete(created.id());

        assertThatThrownBy(() -> service.get(created.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void list는_상태_필터와_페이지_파라미터를_받는다() {
        for (int i = 0; i < 5; i++) {
            service.create("alice-" + i, List.of(item()), null);
        }
        var page = service.list(OrderStatus.PENDING, 0, 3);

        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.items()).hasSize(3);
    }

    @Test
    void list의_page와_size는_검증된다() {
        assertThatThrownBy(() -> service.list(null, -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list(null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list(null, 0, 200))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static OrderItem item() {
        return new OrderItem("sku-1", "도토리", 1, new BigDecimal("1000"));
    }
}
