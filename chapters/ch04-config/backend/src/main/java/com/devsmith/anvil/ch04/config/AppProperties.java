package com.devsmith.anvil.ch04.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code anvil.*} 키들을 타입 안전하게 바인딩하는 record.
 *
 * <p>Spring Boot 3+ 의 권장 패턴 — {@code @ConfigurationProperties} + record + 중첩 record.
 * {@code @Value("${anvil.sender.api-key}")} 를 코드 곳곳에 흩뿌리는 방식과 비교했을 때 이점:
 * <ul>
 *   <li>오타 차단 — yml 키 이름은 한 곳(이 record 의 필드명)에만 존재</li>
 *   <li>타입 안전 — int / boolean / 중첩 객체가 모두 컴파일 타임에 확정</li>
 *   <li>그룹화 — "발송 관련 설정 다 모이는 곳" 같은 응집도</li>
 *   <li>리팩터링 안전 — IDE 가 필드 rename 을 추적</li>
 * </ul>
 *
 * <p>kebab-case (yml) ↔ camelCase (Java) 변환은 Spring 이 자동 처리한다.
 * 즉 {@code anvil.rate-limit.max-per-minute} 는 {@code rateLimit().maxPerMinute()} 으로 노출.</p>
 *
 * <p>{@link com.devsmith.anvil.ch04.Ch04Application} 의 {@code @ConfigurationPropertiesScan}
 * 이 본 record 를 자동 등록한다.</p>
 */
@ConfigurationProperties(prefix = "anvil")
public record AppProperties(
        String name,
        String version,
        String environmentLabel,
        RateLimit rateLimit,
        Sender sender
) {
    /** 발송 한도 정책. {@code maxPerMinute == -1} 이면 무제한(로컬 디버깅용 컨벤션). */
    public record RateLimit(int maxPerMinute, int windowSeconds) { }

    /** 외부 발송 게이트웨이 설정. {@code apiKey} 는 시크릿. */
    public record Sender(String baseUrl, String apiKey) { }
}
