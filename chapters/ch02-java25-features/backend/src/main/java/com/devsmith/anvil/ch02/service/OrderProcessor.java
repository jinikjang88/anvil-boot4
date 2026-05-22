package com.devsmith.anvil.ch02.service;

import com.devsmith.anvil.ch02.domain.OrderRequest;
import com.devsmith.anvil.ch02.domain.OrderResult;
import com.devsmith.anvil.ch02.domain.PaymentGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 주문 배치 처리기.
 *
 * <p>핵심 학습 포인트:
 * <ol>
 *   <li><b>Virtual Thread</b> — 외부 호출(I/O) 이 많을수록 platform 대비 큰 격차</li>
 *   <li><b>Pattern Matching for switch</b> + record deconstruction — sealed 결과를
 *       타입별로 사람-읽기용 설명으로 변환</li>
 *   <li><b>Executor 전략의 분리</b> — 같은 도메인 로직, executor 만 바꿔서 비교</li>
 * </ol>
 * </p>
 */
@Service
public class OrderProcessor {

    /** 사람이 한눈에 결과를 훑을 수 있도록 최대 이 개수까지만 sample 로 노출. */
    private static final int SAMPLE_LIMIT = 10;

    private final PaymentGateway gateway;

    public OrderProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * 동일한 도메인 로직을 주어진 executor 로 fan-out 실행한다.
     *
     * <p>{@link CompletableFuture#supplyAsync(java.util.function.Supplier, java.util.concurrent.Executor)}
     * 로 모든 호출을 동시에 띄운 뒤 {@code join} 으로 수렴 — 결과적으로 전체 elapsed 는
     * <b>가장 느린 한 건의 응답시간</b>에 수렴한다 (executor 가 그만큼 동시성을 허용한다면).</p>
     *
     * @param executorLabel executor 종류를 응답에 표기 (UI 에서 비교용)
     * @param executor      실제 사용할 executor (호출자가 책임지고 close)
     */
    public BatchSummary process(List<OrderRequest> orders,
                                String executorLabel,
                                ExecutorService executor) {

        long startNanos = System.nanoTime();

        List<CompletableFuture<OrderResult>> futures = orders.stream()
                .map(order -> CompletableFuture.supplyAsync(() -> gateway.charge(order), executor))
                .toList();

        List<OrderResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

        return new BatchSummary(
                executorLabel,
                orders.size(),
                elapsedMs,
                countByType(results),
                summarize(results)
        );
    }

    /**
     * Pattern matching for switch — sealed 타입이므로 default 절 불필요.
     * 새 결과 타입이 추가되면 컴파일 에러로 강제 노출됨.
     */
    private static BatchSummary.Counts countByType(List<OrderResult> results) {
        int approved = 0;
        int rejected = 0;
        int pending = 0;
        for (OrderResult result : results) {
            switch (result) {
                case OrderResult.Approved a -> approved++;
                case OrderResult.Rejected r -> rejected++;
                case OrderResult.Pending p  -> pending++;
            }
        }
        return new BatchSummary.Counts(approved, rejected, pending);
    }

    /**
     * record deconstruction — 결과 record 를 패턴으로 분해해 사람-읽기용 문자열 생성.
     * Java 25 까지의 record pattern 표준 문법.
     */
    private static List<String> summarize(List<OrderResult> results) {
        return results.stream()
                .limit(SAMPLE_LIMIT)
                .map(OrderProcessor::describe)
                .toList();
    }

    private static String describe(OrderResult result) {
        return switch (result) {
            case OrderResult.Approved(String id, String txn)        -> "OK  " + id + " -> " + txn;
            case OrderResult.Rejected(String id, String reason)     -> "NG  " + id + " : " + reason;
            case OrderResult.Pending (String id, String reviewTkt)  -> "REV " + id + " ("  + reviewTkt + ")";
        };
    }
}
