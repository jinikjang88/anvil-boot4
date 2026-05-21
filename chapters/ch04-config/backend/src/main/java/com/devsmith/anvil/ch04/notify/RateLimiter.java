package com.devsmith.anvil.ch04.notify;

import com.devsmith.anvil.ch04.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * 환경별 한도를 강제하는 단순 슬라이딩 윈도 카운터.
 *
 * <p>학습 목적상 의도적으로 단순화 — 실 운영에선 Redis 기반 토큰 버킷 / Bucket4j /
 * Resilience4j RateLimiter / API 게이트웨이 (ch11, ch15 에서 다룸) 를 쓴다.</p>
 *
 * <p>핵심은 본 챕터의 학습 포인트: <i>{@link AppProperties} 가 환경마다 다른 한도를 주입</i>
 * 한다는 것. local 에선 -1(무제한), dev 100, prod 10.</p>
 */
@Component
public class RateLimiter {

    private final AppProperties properties;
    private final Object lock = new Object();

    private long windowStartMs;
    private int counter;

    public RateLimiter(AppProperties properties) {
        this.properties = properties;
        this.windowStartMs = System.currentTimeMillis();
    }

    /** true 면 허용. 호출 자체가 카운트를 1 증가시킨다. */
    public boolean tryAcquire() {
        int max = properties.rateLimit().maxPerMinute();
        if (max < 0) {
            return true;   // -1 = 무제한
        }
        long windowMs = properties.rateLimit().windowSeconds() * 1000L;

        synchronized (lock) {
            long now = System.currentTimeMillis();
            if (now - windowStartMs > windowMs) {
                windowStartMs = now;
                counter = 0;
            }
            counter++;
            return counter <= max;
        }
    }

    /** 현재 윈도 안의 누적 호출 수 (UI 표시용). */
    public int currentCount() {
        synchronized (lock) {
            return counter;
        }
    }
}
