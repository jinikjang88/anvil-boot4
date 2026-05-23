package com.devsmith.anvil.ch08.config;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hibernate 의 statement_inspector 프로퍼티에 {@link SqlCapturingInspector} 를 꽂아준다.
 *
 * 생성자에서 SqlCapture 를 받아 static bridge 변수에 세팅.
 * (Hibernate 가 inspector 를 직접 new 하므로 빈 주입 우회.)
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
