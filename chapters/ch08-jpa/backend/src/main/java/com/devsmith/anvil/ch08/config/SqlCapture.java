package com.devsmith.anvil.ch08.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 요청 스레드 단위로 Hibernate 가 발행한 SQL 을 모아 둔다.
 *
 * 학습용 패턴 — 운영 코드에서는 datasource-proxy / p6spy 사용 권장.
 * 컨트롤러가 start() → 서비스 호출 → drain() 순서로 사용.
 */
@Component
public class SqlCapture {

    private static final ThreadLocal<List<String>> BUFFER = ThreadLocal.withInitial(ArrayList::new);

    /** 새 요청 시작 — 버퍼 비움. */
    public void start() {
        BUFFER.get().clear();
    }

    /** Hibernate StatementInspector 가 호출. */
    public void add(String sql) {
        BUFFER.get().add(sql);
    }

    /** 응답 만들 때 호출 — 사본 반환 + ThreadLocal 해제 (메모리 누수 방지). */
    public List<String> drain() {
        List<String> copy = List.copyOf(BUFFER.get());
        BUFFER.remove();
        return copy;
    }
}
