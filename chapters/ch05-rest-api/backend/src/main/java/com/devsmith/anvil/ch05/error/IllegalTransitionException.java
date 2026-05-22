package com.devsmith.anvil.ch05.error;

import com.devsmith.anvil.ch05.domain.OrderStatus;

/**
 * 허용되지 않은 상태 전이 시도 → 409 Conflict.
 *
 * <p>"리소스의 현재 상태와 요청이 충돌" 한다는 의미가 409 의 시맨틱.
 * (잘못된 입력값이면 400/422 가 더 맞음 — ch06 에서 본격 분리.)</p>
 */
public class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(OrderStatus from, OrderStatus to) {
        super("cannot transition from " + from + " to " + to);
    }
}
