package com.devsmith.anvil.ch09b.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 요청 스레드 단위로 발행된 SQL 을 모아 응답에 첨부한다.
 *
 * jOOQ 의 ExecuteListener 가 호출하는 sink — ch08/ch09 의 Hibernate StatementInspector 와
 * 같은 역할이지만 통로가 다르다 (jOOQ 는 빈 주입이 자연스러움).
 */
@Component
public class SqlCapture {

    private static final ThreadLocal<List<String>> BUFFER = ThreadLocal.withInitial(ArrayList::new);

    public void start() {
        BUFFER.get().clear();
    }

    public void add(String sql) {
        BUFFER.get().add(sql);
    }

    public List<String> drain() {
        List<String> copy = List.copyOf(BUFFER.get());
        BUFFER.remove();
        return copy;
    }
}
