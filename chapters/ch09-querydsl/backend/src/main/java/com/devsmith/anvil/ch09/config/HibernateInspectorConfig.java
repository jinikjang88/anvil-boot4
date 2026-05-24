package com.devsmith.anvil.ch09.config;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hibernate 의 statement_inspector 프로퍼티에 {@link SqlCapturingInspector} 를 꽂아준다.
 *
 * Boot 4 패키지 변경 주의 — HibernatePropertiesCustomizer 가
 *   spring-boot-autoconfigure 의 org.springframework.boot.autoconfigure.orm.jpa 에서
 *   spring-boot-hibernate 의 org.springframework.boot.hibernate.autoconfigure 로 이동.
 */
@Configuration
public class HibernateInspectorConfig {

    public HibernateInspectorConfig(SqlCapture capture) {
        SqlCapturingInspector.sink = capture;
    }

    @Bean
    public HibernatePropertiesCustomizer sqlInspectorCustomizer() {
        return properties -> properties.put(
                "hibernate.session_factory.statement_inspector",
                new SqlCapturingInspector()
        );
    }
}
