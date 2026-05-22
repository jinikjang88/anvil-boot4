package com.devsmith.anvil.ch05.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** ch03 의 패턴 — JDK 소유 Clock 을 빈으로 노출해 서비스가 주입받게 한다 (테스트에선 fixed 로 교체). */
@Configuration
public class AppConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
