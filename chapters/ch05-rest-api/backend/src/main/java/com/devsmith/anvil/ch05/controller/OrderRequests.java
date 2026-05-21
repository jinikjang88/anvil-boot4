package com.devsmith.anvil.ch05.controller;

import com.devsmith.anvil.ch05.domain.OrderItem;
import com.devsmith.anvil.ch05.domain.OrderStatus;

import java.util.List;

/**
 * 컨트롤러가 받는 요청 DTO 들을 한 파일에 모았다 (학습 추적성).
 *
 * <p>학습 포인트 — 같은 도메인(Order)의 요청이라도 메서드마다 다른 모양:
 * <ul>
 *   <li>POST {@link Create}            : 생성에 필요한 전체</li>
 *   <li>PATCH {@link PatchMemo}        : 부분 수정 (memo 만)</li>
 *   <li>PUT /{id}/status {@link TransitionStatus} : 단일 필드 교체 (상태)</li>
 * </ul>
 * </p>
 */
public final class OrderRequests {

    private OrderRequests() { }

    public record Create(String customer, List<OrderItem> items, String memo) { }
    public record PatchMemo(String memo) { }
    public record TransitionStatus(OrderStatus status) { }
}
