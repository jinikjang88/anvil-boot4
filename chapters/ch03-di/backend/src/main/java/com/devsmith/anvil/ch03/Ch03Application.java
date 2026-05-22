package com.devsmith.anvil.ch03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ch03-di 진입점.
 * 학습 목적 — 챕터별 독립 실행 가능한 최소 Spring Boot 애플리케이션.
 */
@SpringBootApplication
public class Ch03Application {
    public static void main(String[] args) {
        SpringApplication.run(Ch03Application.class, args);
    }
}
