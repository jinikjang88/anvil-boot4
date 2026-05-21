package com.devsmith.anvil.ch03.formatter;

import org.springframework.stereotype.Component;

/**
 * 메시지 본문 포매팅 — 의존성이 0개인 작은 빈. DI 가 학습 포인트이므로
 * 의존 그래프를 단순하게 유지하기 위해 일부러 가볍게 둔다.
 */
@Component
public class MessageFormatter {

    public String format(String subject, String body) {
        if (subject == null || subject.isBlank()) {
            return body == null ? "" : body.trim();
        }
        return "[" + subject.trim() + "] " + (body == null ? "" : body.trim());
    }
}
