package com.devsmith.anvil.ch09.config;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Hibernate 가 발행하는 모든 JDBC 문장을 가로채 {@link SqlCapture} 에 기록한다.
 *
 * 함정: Hibernate 는 StatementInspector 를 자기가 직접 new 하므로 Spring 빈 주입 불가.
 *      → static volatile 변수로 bridge.
 *      → HibernateInspectorConfig 가 빈 생성 시점에 sink 를 채워준다.
 */
public class SqlCapturingInspector implements StatementInspector {

    private static final long serialVersionUID = 1L;

    static volatile SqlCapture sink;

    @Override
    public String inspect(String sql) {
        SqlCapture s = sink;
        if (s != null) {
            s.add(sql);
        }
        return sql;
    }
}
