package com.devsmith.anvil.ch09b.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "anvil.seed")
public record SeedProperties(boolean enabled) {}
