package com.devsmith.anvil.ch04.secret;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecretMasker")
class SecretMaskerTest {

    private final SecretMasker masker = new SecretMasker();

    @Test
    void null과_짧은_값은_별표_4개로_마스킹된다() {
        assertThat(masker.mask(null)).isEqualTo("****");
        assertThat(masker.mask("")).isEqualTo("****");
        assertThat(masker.mask("abc")).isEqualTo("****");
    }

    @Test
    void 짧은_값은_끝_2글자만_노출() {
        assertThat(masker.mask("12345678")).isEqualTo("****78");
    }

    @Test
    void 긴_값은_앞_3과_끝_4를_노출하고_가운데를_마스킹() {
        assertThat(masker.mask("sk-localdev-1234567890abcd")).isEqualTo("sk-****abcd");
    }

    @Test
    void api_key_같은_키는_sensitive로_판별() {
        assertThat(masker.isSensitive("anvil.sender.apiKey")).isTrue();
        assertThat(masker.isSensitive("anvil.sender.api-key")).isTrue();
        assertThat(masker.isSensitive("PASSWORD")).isTrue();
        assertThat(masker.isSensitive("user.token")).isTrue();
    }

    @Test
    void 평범한_키는_sensitive_아님() {
        assertThat(masker.isSensitive("anvil.name")).isFalse();
        assertThat(masker.isSensitive("server.port")).isFalse();
        assertThat(masker.isSensitive(null)).isFalse();
    }
}
