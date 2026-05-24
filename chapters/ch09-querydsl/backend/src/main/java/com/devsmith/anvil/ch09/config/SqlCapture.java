package com.devsmith.anvil.ch09.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 요청 스레드 단위로 Hibernate 가 발행한 SQL 을 모아 응답에 첨부한다.
 *
 * 프론트가 "Spring Data 메서드명 / JPQL 문자열 / QueryDSL" 세 모드의 SQL 을
 * 사이드바이사이드로 보여줄 수 있게 하는 인프라.
 *
 * 학습용 패턴. 운영에서는 datasource-proxy / p6spy.
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
