package com.devsmith.anvil.ch07.domain;

import java.time.Instant;

/**
 * 사용자 도메인.
 *
 * <p>학습 포인트 — DB 에 저장되는 형태:
 * <ul>
 *   <li>{@code passwordHash} — BCrypt 해시 (절대 평문 / 단순 SHA 금지)</li>
 *   <li>{@code phoneNumberEncrypted} — AES 암호화된 전화번호 (PII)</li>
 *   <li>응답 변환 시 {@code passwordHash} 는 절대 노출 X, {@code phoneNumber} 는 마스킹</li>
 * </ul>
 * </p>
 */
public record User(
        String id,
        String email,
        String passwordHash,
        String name,
        String phoneNumberEncrypted,
        UserRole role,
        Instant createdAt
) {

    public User {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash required");
        }
        if (role == null) {
            throw new IllegalArgumentException("role required");
        }
    }
}
