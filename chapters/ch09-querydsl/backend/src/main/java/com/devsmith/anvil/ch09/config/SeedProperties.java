package com.devsmith.anvil.ch09.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 시드 활성화 여부 — local 프로필에서만 켜고 테스트에서는 끈다.
 * application.yml 의 anvil.seed.* 와 매핑.
 */
@ConfigurationProperties(prefix = "anvil.seed")
public record SeedProperties(boolean enabled) {}
