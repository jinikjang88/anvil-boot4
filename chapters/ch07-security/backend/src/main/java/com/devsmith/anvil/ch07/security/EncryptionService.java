package com.devsmith.anvil.ch07.security;

import com.devsmith.anvil.ch07.config.SecurityProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 양방향 암호화.
 *
 * <p>학습 포인트:
 * <ul>
 *   <li><b>왜 GCM</b> — Authenticated Encryption. CBC + 별도 HMAC 조합보다 안전하고, 한 호출에 무결성 보장</li>
 *   <li><b>매번 새 IV</b> — 같은 평문도 매번 다른 ciphertext (운영에서 결정적 암호화 금지)</li>
 *   <li><b>저장 형식</b> {@code base64(IV) ":" base64(ciphertext+tag)} — IV 는 비밀이 아니지만 매번 달라야 함</li>
 * </ul>
 * </p>
 *
 * <p>이 클래스는 <i>학습용 데모</i>. 운영에선 KMS / HSM / Vault 통합 권장.</p>
 */
@Component
public class EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;          // GCM 권장
    private static final int TAG_BITS = 128;
    private static final String SEPARATOR = ":";

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    public EncryptionService(SecurityProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.encryption().masterKey());
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "anvil.security.encryption.master-key 는 32바이트(base64) 여야 합니다 — 받은 길이: " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes("UTF-8"));

            return Base64.getEncoder().encodeToString(iv)
                    + SEPARATOR
                    + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        try {
            String[] parts = encrypted.split(SEPARATOR, 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("ciphertext format invalid (expected IV:cipher)");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("decryption failed", e);
        }
    }

    /** 응답용 — 010-1234-5678 → 010-****-5678. */
    public static String maskPhone(String plain) {
        if (plain == null || plain.length() < 4) {
            return "****";
        }
        return plain.substring(0, Math.min(3, plain.length()))
                + "-****-"
                + plain.substring(plain.length() - 4);
    }
}
