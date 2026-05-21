package com.devsmith.anvil.ch02.domain;

/**
 * 주문 처리 결과.
 *
 * <p>Java 25 <b>sealed interface</b> — 결과 유형이 {@link Approved} / {@link Rejected} /
 * {@link Pending} 셋으로 <i>완전히 닫혀 있음</i>을 컴파일러에 명시한다.
 * 새 결과 타입이 늘면 (예: {@code Refunded}) <b>모든</b> switch 분기가
 * 컴파일 에러로 잡힌다 — 누락 분기로 인한 버그 차단.</p>
 *
 * <p>같은 컴파일 단위(이 파일) 안의 nested record 들은 자동으로 permitted 이므로
 * 별도 {@code permits} 절을 쓰지 않는다 (Java 17+ 동작).</p>
 */
public sealed interface OrderResult {

    String orderId();

    /** 결제 승인 — transactionId 는 외부 PG 가 부여한 거래 식별자. */
    record Approved(String orderId, String transactionId) implements OrderResult { }

    /** 결제 거절 — reason 으로 거절 사유를 전달. */
    record Rejected(String orderId, String reason) implements OrderResult { }

    /** 결제 보류 — 수동 검토가 필요한 케이스 (예: 고액). */
    record Pending(String orderId, String reviewTicket) implements OrderResult { }
}
