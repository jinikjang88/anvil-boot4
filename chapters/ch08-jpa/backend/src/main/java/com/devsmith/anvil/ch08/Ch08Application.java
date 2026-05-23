package com.devsmith.anvil.ch08;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ch08-jpa 진입점.
 * 학습 목적 — 챕터별 독립 실행 가능한 최소 Spring Boot 애플리케이션.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Ch08Application {
    public static void main(String[] args) {
        SpringApplication.run(Ch08Application.class, args);
    }
}
