package com.devsmith.anvil.ch02.domain;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * 가짜 외부 결제 게이트웨이.
 *
 * <p>실제 PG/은행망 호출처럼 <b>I/O 블로킹</b>(200ms) 이 발생한다고 가정한다.
 * 이 sleep 이 Virtual Thread 데모의 핵심 — Virtual Thread 는 sleep 중에 캐리어 스레드를
 * 양보(parking) 하므로 N 개의 동시 호출이 거의 동시에 끝난다.</p>
 *
 * <p>결과는 금액에 따라 결정 (학습용 단순 규칙):
 * <ul>
 *   <li>amount &le; 0 → Rejected</li>
 *   <li>amount &gt; 1,000,000 → Pending (수동 검토)</li>
 *   <li>그 외 → Approved</li>
 * </ul>
 * </p>
 */
@Component
public class PaymentGateway {

    /** 외부 호출 지연 시뮬레이션. 너무 짧으면 차이가 안 드러나고, 길면 데모가 답답해서 200ms. */
    static final Duration EXTERNAL_LATENCY = Duration.ofMillis(200);

    private static final BigDecimal MANUAL_REVIEW_THRESHOLD = new BigDecimal("1000000");

    public OrderResult charge(OrderRequest request) {
        sleepQuietly(EXTERNAL_LATENCY);

        if (request.amount().signum() <= 0) {
            return new OrderResult.Rejected(request.orderId(), "amount must be positive");
        }
        if (request.amount().compareTo(MANUAL_REVIEW_THRESHOLD) > 0) {
            return new OrderResult.Pending(request.orderId(), "review-" + request.orderId());
        }
        return new OrderResult.Approved(request.orderId(), "tx-" + request.orderId());
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // 인터럽트 상태 복원 — Virtual Thread 든 Platform Thread 든 일관되게 동작
            Thread.currentThread().interrupt();
        }
    }
}
