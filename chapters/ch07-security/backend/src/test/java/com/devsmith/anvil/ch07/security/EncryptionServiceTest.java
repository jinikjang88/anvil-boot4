package com.devsmith.anvil.ch07.security;

import com.devsmith.anvil.ch07.config.SecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EncryptionService — AES-256-GCM")
class EncryptionServiceTest {

    private static final String KEY_32B =
            "9aE1bC2dF3gH4iJ5kL6mN7oP8qR9sT0uvWxYzABCDEF=";   // base64, 32바이트

    private final EncryptionService service = new EncryptionService(
            new SecurityProperties(null, new SecurityProperties.Encryption(KEY_32B)));

    @Test
    void 암복호화_왕복이_평문을_복원한다() {
        String plain = "010-1234-5678";

        String cipher = service.encrypt(plain);
        String back   = service.decrypt(cipher);

        assertThat(back).isEqualTo(plain);
    }

    @Test
    void 같은_평문도_매번_다른_ciphertext_가_나온다() {
        String plain = "010-1234-5678";

        String c1 = service.encrypt(plain);
        String c2 = service.encrypt(plain);

        // GCM 의 무작위 IV 때문에 결정적 암호화가 아니어야 함
        assertThat(c1).isNotEqualTo(c2);
        assertThat(service.decrypt(c1)).isEqualTo(plain);
        assertThat(service.decrypt(c2)).isEqualTo(plain);
    }

    @Test
    void null은_그대로_null() {
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    void 잘못된_키_길이는_생성에서_실패한다() {
        SecurityProperties bad = new SecurityProperties(
                null, new SecurityProperties.Encryption("c2hvcnQ="));   // "short" base64 — 5 bytes

        assertThatThrownBy(() -> new EncryptionService(bad))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32바이트");
    }

    @Test
    void maskPhone는_가운데를_가린다() {
        assertThat(EncryptionService.maskPhone("010-1234-5678")).isEqualTo("010-****-5678");
        assertThat(EncryptionService.maskPhone(null)).isEqualTo("****");
    }
}
