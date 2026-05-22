package com.devsmith.anvil.ch05.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderStateMachine — 상태 전이 규칙")
class OrderStateMachineTest {

    private final OrderStateMachine machine = new OrderStateMachine();

    @Test
    void PENDING에서_CONFIRMED_또는_CANCELLED로_갈_수_있다() {
        assertThat(machine.canTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED)).isTrue();
        assertThat(machine.canTransition(OrderStatus.PENDING, OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void PENDING에서_DELIVERED로는_바로_못_간다() {
        assertThat(machine.canTransition(OrderStatus.PENDING, OrderStatus.DELIVERED)).isFalse();
        assertThat(machine.canTransition(OrderStatus.PENDING, OrderStatus.SHIPPED)).isFalse();
    }

    @Test
    void 종료_상태_DELIVERED는_어디로도_못_간다() {
        for (OrderStatus to : OrderStatus.values()) {
            assertThat(machine.canTransition(OrderStatus.DELIVERED, to))
                    .as("DELIVERED -> %s", to)
                    .isFalse();
        }
    }

    @Test
    void CANCELLED도_종료_상태_어디로도_못_간다() {
        for (OrderStatus to : OrderStatus.values()) {
            assertThat(machine.canTransition(OrderStatus.CANCELLED, to))
                    .as("CANCELLED -> %s", to)
                    .isFalse();
        }
    }

    @Test
    void 자기_자신으로의_전이는_차단() {
        for (OrderStatus s : OrderStatus.values()) {
            assertThat(machine.canTransition(s, s))
                    .as("self-transition %s", s)
                    .isFalse();
        }
    }

    @Test
    void null은_안전하게_거짓을_반환한다() {
        assertThat(machine.canTransition(null, OrderStatus.PENDING)).isFalse();
        assertThat(machine.canTransition(OrderStatus.PENDING, null)).isFalse();
    }
}
