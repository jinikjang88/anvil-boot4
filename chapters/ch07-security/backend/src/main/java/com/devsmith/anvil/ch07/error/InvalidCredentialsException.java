package com.devsmith.anvil.ch07.error;

/**
 * 로그인 실패. <b>이메일 존재 여부 / 비밀번호 불일치를 구분하지 않는다</b> —
 * 사용자 열거(enumeration) 공격을 막기 위한 표준 패턴.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다");
    }
}
