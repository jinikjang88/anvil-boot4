package com.devsmith.anvil.ch07.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * {@code anvil.security.*} 키 바인딩. ch04 의 {@code @ConfigurationProperties} 패턴 재사용.
 */
@ConfigurationProperties(prefix = "anvil.security")
public record SecurityProperties(
        Jwt jwt,
        Encryption encryption
) {
    public record Jwt(
            String secret,
            int expirationSeconds,
            String issuer
    ) {
        public Duration expiration() {
            return Duration.ofSeconds(expirationSeconds);
        }
    }

    public record Encryption(String masterKey) { }
}
