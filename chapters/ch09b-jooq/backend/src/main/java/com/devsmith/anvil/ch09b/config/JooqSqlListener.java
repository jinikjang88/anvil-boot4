package com.devsmith.anvil.ch09b.config;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jOOQ 가 발행하는 SQL 을 가로채 {@link SqlCapture} 에 적재.
 *
 * Hibernate StatementInspector 와 달리 jOOQ ExecuteListener 는 Spring 빈으로 자연스럽게 주입된다.
 * (jOOQ 가 직접 new 하지 않고, jOOQ Configuration 의 ExecuteListenerProvider 를 통해 받는다.)
 *
 * 학습용 — 운영에서는 jOOQ 의 logging executor 또는 datasource-proxy 권장.
 */
@Configuration
public class JooqSqlListener {

    /**
     * ExecuteListenerProvider 를 빈으로 등록하면 Boot 의 jOOQ AutoConfiguration 이
     * DSLContext 의 Configuration 에 자동으로 꽂아준다.
     */
    @Bean
    ExecuteListenerProvider sqlCaptureListenerProvider(SqlCapture capture) {
        return () -> new ExecuteListener() {
            @Override
            public void executeStart(ExecuteContext ctx) {
                // 바인딩 파라미터까지 인라인된 형태로 캡처 — 학습 가시성 우선.
                String sql = ctx.query() != null ? ctx.query().getSQL() : ctx.sql();
                if (sql != null && !sql.isBlank()) {
                    capture.add(sql);
                }
            }
        };
    }
}
