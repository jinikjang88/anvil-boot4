package com.devsmith.anvil.ch07.domain;

/**
 * 사용자 역할. Spring Security 컨벤션 — 권한은 {@code ROLE_USER}, {@code ROLE_ADMIN} 형태로 등록되고,
 * {@code @PreAuthorize("hasRole('ADMIN')")} 는 {@code ROLE_} prefix 를 자동으로 붙여 비교한다.
 */
public enum UserRole {
    USER,
    ADMIN;

    /** Spring Security 권한 문자열로 변환. */
    public String asAuthority() {
        return "ROLE_" + name();
    }
}
