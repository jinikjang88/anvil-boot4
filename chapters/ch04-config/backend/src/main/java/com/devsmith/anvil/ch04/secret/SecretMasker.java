package com.devsmith.anvil.ch04.secret;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 시크릿 마스킹 유틸.
 *
 * <p>응답이나 로그에 시크릿 원본이 새지 않도록 가공한다. 단, *마스킹은 최후의 방어선* 이지
 * 시크릿을 안전하게 보관하는 수단이 아니다 — 본질은 (1) 저장소에 안 두기, (2) 노출 경로 제한.</p>
 *
 * <p>식별자: key 이름이 {@link #SENSITIVE_KEY_TOKENS} 중 하나를 부분 포함하면 sensitive 로 간주.
 * 운영에선 화이트리스트 방식이 더 안전하지만, 학습용으로 단순화.</p>
 */
@Component
public class SecretMasker {

    private static final Set<String> SENSITIVE_KEY_TOKENS = Set.of(
            "apikey", "api-key", "password", "secret", "token", "credential"
    );

    /** 키 이름만으로 sensitive 인지 판별. 값은 보지 않는다 (값 패턴 추측은 오탐이 잦음). */
    public boolean isSensitive(String key) {
        if (key == null) return false;
        String normalized = key.toLowerCase();
        return SENSITIVE_KEY_TOKENS.stream().anyMatch(normalized::contains);
    }

    /**
     * 값 길이에 따라 마스킹 형태를 조절한다.
     * <ul>
     *   <li>null / 짧은 값: {@code ****}</li>
     *   <li>8 자 이하: {@code ****xx}</li>
     *   <li>그 외: 앞 3 + {@code ****} + 끝 4 (예: {@code sk-****abcd})</li>
     * </ul>
     */
    public String mask(String value) {
        if (value == null || value.length() < 4) {
            return "****";
        }
        if (value.length() <= 8) {
            return "****" + value.substring(value.length() - 2);
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
