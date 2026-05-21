package com.devsmith.anvil.ch02.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentGateway — 결제 규칙")
class PaymentGatewayTest {

    private final PaymentGateway gateway = new PaymentGateway();

    @Test
    void 정상_금액이면_Approved를_돌려준다() {
        // given
        OrderRequest order = new OrderRequest("order-1", new BigDecimal("15000"));

        // when
        OrderResult result = gateway.charge(order);

        // then — pattern matching 으로 결과 타입 확인
        assertThat(result).isInstanceOf(OrderResult.Approved.class);
        OrderResult.Approved approved = (OrderResult.Approved) result;
        assertThat(approved.orderId()).isEqualTo("order-1");
        assertThat(approved.transactionId()).isEqualTo("tx-order-1");
    }

    @Test
    void amount가_0이하면_Rejected를_돌려준다() {
        // given
        OrderRequest order = new OrderRequest("order-2", BigDecimal.ZERO);

        // when
        OrderResult result = gateway.charge(order);

        // then
        assertThat(result).isInstanceOf(OrderResult.Rejected.class);
        OrderResult.Rejected rejected = (OrderResult.Rejected) result;
        assertThat(rejected.reason()).contains("positive");
    }

    @Test
    void amount가_백만초과면_Pending으로_수동검토에_들어간다() {
        // given
        OrderRequest order = new OrderRequest("order-3", new BigDecimal("1000001"));

        // when
        OrderResult result = gateway.charge(order);

        // then
        assertThat(result).isInstanceOf(OrderResult.Pending.class);
        OrderResult.Pending pending = (OrderResult.Pending) result;
        assertThat(pending.reviewTicket()).isEqualTo("review-order-3");
    }
}
