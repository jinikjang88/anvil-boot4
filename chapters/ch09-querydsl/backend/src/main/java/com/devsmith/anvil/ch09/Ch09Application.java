package com.devsmith.anvil.ch09;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ch09-querydsl 진입점.
 *
 * 학습 목적: 같은 게시판 도메인 + 같은 검색 요구사항을
 *   1) 메서드 이름 폭발 (Spring Data)
 *   2) JPQL String concat (취약)
 *   3) QueryDSL (타입 세이프 동적 쿼리)
 * 세 가지로 구현해 비교한다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Ch09Application {
    public static void main(String[] args) {
        SpringApplication.run(Ch09Application.class, args);
    }
}
