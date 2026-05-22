package com.devsmith.anvil.ch02.service;

import com.devsmith.anvil.ch02.domain.OrderRequest;
import com.devsmith.anvil.ch02.domain.PaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderProcessor 단위 테스트.
 * <p>실제 {@link PaymentGateway} 를 그대로 쓴다 — sleep 이 짧고(200ms) 동시성 효과를
 * 직접 측정하는 것이 이 테스트의 목적이기 때문이다.</p>
 */
@DisplayName("OrderProcessor — 배치 처리")
class OrderProcessorTest {

    private final OrderProcessor processor = new OrderProcessor(new PaymentGateway());

    @Test
    void 결과의_총_개수와_분류_합이_입력과_일치한다() {
        // given — 20 건: 정상 18 / 거절 1 (amount=0) / 보류 1 (amount=1000001)
        List<OrderRequest> orders = IntStream.range(0, 18)
                .mapToObj(i -> new OrderRequest("ok-" + i, new BigDecimal("15000")))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        orders.add(new OrderRequest("rj-0", new BigDecimal("0.0001")
                .setScale(0, java.math.RoundingMode.DOWN)));   // 0 으로 절삭 → rejected
        orders.add(new OrderRequest("pd-0", new BigDecimal("1000001")));

        // when
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            BatchSummary summary = processor.process(orders, "virtual", executor);

            // then
            assertThat(summary.total()).isEqualTo(20);
            int sum = summary.counts().approved()
                    + summary.counts().rejected()
                    + summary.counts().pending();
            assertThat(sum).isEqualTo(20);
            assertThat(summary.counts().rejected()).isEqualTo(1);
            assertThat(summary.counts().pending()).isEqualTo(1);
            assertThat(summary.counts().approved()).isEqualTo(18);
        }
    }

    @Test
    void Virtual_Thread는_Platform보다_훨씬_빠르다() {
        // given — 50 건 × 200ms 외부호출
        List<OrderRequest> orders = IntStream.range(0, 50)
                .mapToObj(i -> new OrderRequest("o-" + i, new BigDecimal("15000")))
                .toList();

        // when
        BatchSummary platformSummary;
        BatchSummary virtualSummary;
        try (ExecutorService platform = Executors.newFixedThreadPool(10)) {
            platformSummary = processor.process(orders, "platform", platform);
        }
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            virtualSummary = processor.process(orders, "virtual", virtual);
        }

        // then — Platform: 5 라운드 × 200ms ≈ 1000ms 이상
        //        Virtual : 거의 한 라운드 ≈ 250ms 안팎
        assertThat(platformSummary.elapsedMs()).isGreaterThan(900);
        assertThat(virtualSummary.elapsedMs()).isLessThan(500);
        assertThat(virtualSummary.elapsedMs()).isLessThan(platformSummary.elapsedMs() / 2);
    }

    @Test
    void samples는_최대_10건만_담는다() {
        // given
        List<OrderRequest> orders = IntStream.range(0, 30)
                .mapToObj(i -> new OrderRequest("o-" + i, new BigDecimal("15000")))
                .toList();

        // when
        BatchSummary summary;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            summary = processor.process(orders, "virtual", executor);
        }

        // then
        assertThat(summary.samples()).hasSize(10);
        assertThat(summary.samples()).allMatch(s -> s.startsWith("OK")
                                                 || s.startsWith("NG")
                                                 || s.startsWith("REV"));
    }
}
