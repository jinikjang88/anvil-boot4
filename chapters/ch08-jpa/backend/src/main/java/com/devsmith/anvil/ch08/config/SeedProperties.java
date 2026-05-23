package com.devsmith.anvil.ch08.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 시드 데이터 설정 — anvil.seed.* 네임스페이스.
 */
@ConfigurationProperties(prefix = "anvil.seed")
public record SeedProperties(
        boolean enabled,
        int postCount,
        int minComments,
        int maxComments
) {
    public SeedProperties {
        if (postCount < 0) throw new IllegalArgumentException("postCount >= 0");
        if (minComments < 0 || maxComments < minComments) {
            throw new IllegalArgumentException("0 <= minComments <= maxComments");
        }
    }
}
