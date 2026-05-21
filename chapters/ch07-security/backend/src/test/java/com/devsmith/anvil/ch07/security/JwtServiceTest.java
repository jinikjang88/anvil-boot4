package com.devsmith.anvil.ch07.security;

import com.devsmith.anvil.ch07.config.SecurityProperties;
import com.devsmith.anvil.ch07.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService — HS256")
class JwtServiceTest {

    private static final String SECRET =
            "kQ5pT8zR3vL9xH7nB2cF1mY4wK6jD0aE3gP7sQuV8xY=";    // 32바이트 base64

    private JwtService serviceAt(Instant now) {
        SecurityProperties props = new SecurityProperties(
                new SecurityProperties.Jwt(SECRET, 3600, "anvil-boot4"),
                null);
        return new JwtService(props, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void 발급한_토큰은_같은_키로_검증_가능하고_클레임이_보존된다() {
        JwtService service = serviceAt(Instant.parse("2026-05-21T00:00:00Z"));

        JwtService.Issued issued = service.issue("user-1", "u@x.com", UserRole.USER);
        JwtService.Verified verified = service.verify(issued.token());

        assertThat(verified.userId()).isEqualTo("user-1");
        assertThat(verified.email()).isEqualTo("u@x.com");
        assertThat(verified.role()).isEqualTo(UserRole.USER);
        assertThat(verified.expiresAt()).isEqualTo(Instant.parse("2026-05-21T01:00:00Z"));
    }

    @Test
    void 만료된_토큰은_검증에서_실패한다() {
        Instant issueTime = Instant.parse("2026-05-21T00:00:00Z");
        JwtService issuer = serviceAt(issueTime);
        JwtService.Issued issued = issuer.issue("user-2", "u@x.com", UserRole.USER);

        // 시계를 2시간 뒤로 — 만료(1시간) 후
        JwtService verifier = serviceAt(issueTime.plusSeconds(7200));

        assertThatThrownBy(() -> verifier.verify(issued.token()))
                .hasMessageContaining("JWT expired");
    }

    @Test
    void 다른_키로_검증하면_서명_불일치() {
        Instant now = Instant.parse("2026-05-21T00:00:00Z");
        JwtService issuer = serviceAt(now);
        JwtService.Issued issued = issuer.issue("user-3", "u@x.com", UserRole.ADMIN);

        // 다른 32바이트 base64 secret 으로 verifier 생성
        SecurityProperties other = new SecurityProperties(
                new SecurityProperties.Jwt(
                        "E6zKy2QWf5sHsIjd+AiSTBGbrHITlFsvFVroDSDT4II=",
                        3600, "anvil-boot4"),
                null);
        JwtService otherVerifier = new JwtService(other, Clock.fixed(now, ZoneOffset.UTC));

        assertThatThrownBy(() -> otherVerifier.verify(issued.token()))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    void issuer_가_불일치하면_검증_실패() {
        Instant now = Instant.parse("2026-05-21T00:00:00Z");
        JwtService issuer = serviceAt(now);
        JwtService.Issued issued = issuer.issue("user-4", "u@x.com", UserRole.USER);

        // 다른 issuer 를 기대하는 verifier
        SecurityProperties otherIssuer = new SecurityProperties(
                new SecurityProperties.Jwt(SECRET, 3600, "different-system"),
                null);
        JwtService verifier = new JwtService(otherIssuer, Clock.fixed(now, ZoneOffset.UTC));

        assertThatThrownBy(() -> verifier.verify(issued.token()))
                .isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
    }

    @Test
    void 짧은_secret_은_생성에서_실패한다() {
        SecurityProperties bad = new SecurityProperties(
                new SecurityProperties.Jwt("c2hvcnQ=", 3600, "anvil"),    // "short" base64
                null);
        Clock clock = Clock.fixed(Instant.parse("2026-05-21T00:00:00Z"), ZoneOffset.UTC);

        assertThatThrownBy(() -> new JwtService(bad, clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32바이트");
    }
}
