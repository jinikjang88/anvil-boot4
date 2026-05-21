package com.devsmith.anvil.ch02.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderRequest record")
class OrderRequestTest {

    @Test
    void orderId가_null이면_생성에서_실패한다() {
        assertThatThrownBy(() -> new OrderRequest(null, new BigDecimal("1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId");
    }

    @Test
    void orderId가_빈_문자열이면_생성에서_실패한다() {
        assertThatThrownBy(() -> new OrderRequest("   ", new BigDecimal("1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId");
    }

    @Test
    void amount가_null이면_생성에서_실패한다() {
        assertThatThrownBy(() -> new OrderRequest("order-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void 정상_입력이면_record가_그대로_보존된다() {
        // given
        BigDecimal amount = new BigDecimal("15000");

        // when
        OrderRequest request = new OrderRequest("order-42", amount);

        // then
        assertThat(request.orderId()).isEqualTo("order-42");
        assertThat(request.amount()).isEqualByComparingTo(amount);
    }
}
