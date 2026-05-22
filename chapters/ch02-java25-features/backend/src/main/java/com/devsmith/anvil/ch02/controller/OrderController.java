package com.devsmith.anvil.ch02.controller;

import com.devsmith.anvil.ch02.domain.OrderRequest;
import com.devsmith.anvil.ch02.service.BatchSummary;
import com.devsmith.anvil.ch02.service.OrderProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Virtual Thread vs Platform Thread 비교 데모.
 *
 * <pre>
 *   POST /api/orders/process-platform   → 고정 10 스레드 풀로 처리
 *   POST /api/orders/process-virtual    → Virtual Thread 로 처리
 * </pre>
 *
 * 같은 요청을 두 엔드포인트에 던지면, 외부 호출(200ms)이 많을수록 virtual 이 압도적으로 빠르다.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /** 운영 웹앱에서 흔히 보는 worker pool 크기 — 이 한도가 platform 의 병목이다. */
    private static final int PLATFORM_POOL_SIZE = 10;

    private final OrderProcessor processor;

    public OrderController(OrderProcessor processor) {
        this.processor = processor;
    }

    @PostMapping("/process-platform")
    public BatchSummary processWithPlatform(@RequestBody BatchRequest request) {
        try (ExecutorService executor = Executors.newFixedThreadPool(PLATFORM_POOL_SIZE)) {
            return processor.process(generate(request), "platform", executor);
        }
    }

    @PostMapping("/process-virtual")
    public BatchSummary processWithVirtual(@RequestBody BatchRequest request) {
        // Java 21+ 표준 API — task 당 새 virtual thread 를 생성. 풀 크기 개념 자체가 없음.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return processor.process(generate(request), "virtual", executor);
        }
    }

    /**
     * 요청 파라미터로 합성 주문을 N 건 생성한다.
     * 실데이터를 흉내내려고 임계치(1,000,000) 근처/이하에 골고루 분포하도록 단순 난수 사용.
     */
    private static List<OrderRequest> generate(BatchRequest request) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        BigDecimal min = request.amountMin();
        BigDecimal max = request.amountMax();
        BigDecimal range = max.subtract(min);

        List<OrderRequest> orders = new ArrayList<>(request.count());
        for (int i = 0; i < request.count(); i++) {
            BigDecimal amount = min.add(
                    range.multiply(BigDecimal.valueOf(random.nextDouble()))
                         .setScale(0, RoundingMode.HALF_UP)
            );
            orders.add(new OrderRequest("order-%04d".formatted(i), amount));
        }
        return orders;
    }

    /**
     * record compact constructor 가 던지는 IllegalArgumentException 을 400 으로 변환.
     * (ch06-validation 에서 Problem Details 기반 본격 처리를 다룬다.)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
