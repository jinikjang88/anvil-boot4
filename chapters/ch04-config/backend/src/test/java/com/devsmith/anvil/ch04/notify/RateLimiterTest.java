package com.devsmith.anvil.ch04.notify;

import com.devsmith.anvil.ch04.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimiter")
class RateLimiterTest {

    @Test
    void max가_minus_1이면_몇번_호출해도_모두_허용() {
        // given
        RateLimiter limiter = new RateLimiter(properties(-1, 60));

        // when / then
        for (int i = 0; i < 1000; i++) {
            assertThat(limiter.tryAcquire()).as("호출 #%d", i).isTrue();
        }
    }

    @Test
    void max가_3이면_4번째부터_거절() {
        RateLimiter limiter = new RateLimiter(properties(3, 60));

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void 윈도가_지나면_카운터가_초기화된다() throws InterruptedException {
        // window = 1초로 짧게
        RateLimiter limiter = new RateLimiter(properties(2, 1));

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();

        Thread.sleep(1100); // 윈도 경과

        assertThat(limiter.tryAcquire()).isTrue();
    }

    private static AppProperties properties(int max, int windowSeconds) {
        return new AppProperties(
                "anvil-boot4",
                "0.4.0",
                "test",
                new AppProperties.RateLimit(max, windowSeconds),
                new AppProperties.Sender("http://test", "test-key")
        );
    }
}
