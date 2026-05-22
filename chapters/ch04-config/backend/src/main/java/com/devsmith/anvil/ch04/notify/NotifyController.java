package com.devsmith.anvil.ch04.notify;

import com.devsmith.anvil.ch04.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 환경별 한도를 적용한 알림 발송 (mock).
 *
 * <p>이 챕터의 시연 포인트: 같은 코드, 같은 엔드포인트, 그러나
 * <b>profile 만 바꾸면 한도가 달라진다</b>. 코드 변경 0줄.</p>
 */
@RestController
public class NotifyController {

    private final RateLimiter limiter;
    private final AppProperties properties;

    public NotifyController(RateLimiter limiter, AppProperties properties) {
        this.limiter = limiter;
        this.properties = properties;
    }

    @PostMapping("/api/notify")
    public ResponseEntity<Map<String, Object>> notify(@RequestBody NotifyRequest request) {
        if (!limiter.tryAcquire()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "rate limit exceeded",
                    "maxPerMinute", properties.rateLimit().maxPerMinute(),
                    "windowSeconds", properties.rateLimit().windowSeconds()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "recipient", request.recipient(),
                "message", request.message(),
                "delivered", true,
                "currentCount", limiter.currentCount()
        ));
    }

    public record NotifyRequest(String recipient, String message) { }
}
