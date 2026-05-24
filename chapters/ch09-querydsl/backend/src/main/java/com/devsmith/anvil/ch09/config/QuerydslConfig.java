package com.devsmith.anvil.ch09.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 핵심 빈 — JPAQueryFactory.
 *
 * 함정: 이 빈을 등록하지 않으면 PostQueryRepositoryImpl 에서
 *      "No qualifying bean of type 'JPAQueryFactory'" 로 부팅 실패.
 *      Spring Data 가 자동으로 만들어주지 않는다.
 *
 * EntityManager 는 트랜잭션 프록시 — 호출 시점의 트랜잭션에 자동으로 붙는다.
 */
@Configuration
public class QuerydslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}
