package com.devsmith.anvil.ch07.security;

import com.devsmith.anvil.ch07.config.SecurityProperties;
import com.devsmith.anvil.ch07.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 발급 / 검증.
 *
 * <p>알고리즘: HS256 (HMAC + SHA-256). 단일 시스템 내 인증에 적합 — 비대칭(RS256) 은
 * 외부 검증자가 있는 경우 (OAuth2 IdP 등).</p>
 *
 * <p>클레임 구성:
 * <ul>
 *   <li>{@code sub} (subject) — userId</li>
 *   <li>{@code email}, {@code role} — 커스텀 클레임</li>
 *   <li>{@code iat}, {@code exp} — 발급/만료 시점 (jjwt 가 강제 검증)</li>
 *   <li>{@code iss} — issuer (검증 시 일치 강제)</li>
 * </ul>
 * </p>
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(SecurityProperties properties, Clock clock) {
        byte[] secretBytes = Base64.getDecoder().decode(properties.jwt().secret());
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "anvil.security.jwt.secret 은 32바이트 이상(base64) 이어야 합니다 (HS256) — 받은 길이: "
                            + secretBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = properties.jwt().issuer();
        this.expiration = properties.jwt().expiration();
        this.clock = clock;
    }

    public Issued issue(String userId, String email, UserRole role) {
        Instant now = Instant.now(clock);
        Instant exp = now.plus(expiration);

        String token = Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("email", email)
                .claim("role", role.name())
                .signWith(signingKey)
                .compact();

        return new Issued(token, expiration.toSeconds(), exp);
    }

    /**
     * 토큰 검증 + 클레임 추출. 서명 위조 / 만료 / issuer 불일치 시 예외 발생.
     */
    public Verified verify(String token) {
        Jws<Claims> jws = Jwts.parser()
                .clock(jjwtClockAdapter())   // 만료 검증도 우리 Clock 으로 — 테스트가 결정적
                .requireIssuer(issuer)
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);

        Claims body = jws.getPayload();
        return new Verified(
                body.getSubject(),
                body.get("email", String.class),
                UserRole.valueOf(body.get("role", String.class)),
                body.getExpiration().toInstant()
        );
    }

    /** jjwt 의 시간 기준을 우리 java.time.Clock 으로 위임. */
    private io.jsonwebtoken.Clock jjwtClockAdapter() {
        Clock self = this.clock;
        return () -> Date.from(Instant.now(self));
    }

    public record Issued(String token, long expiresInSeconds, Instant expiresAt) { }
    public record Verified(String userId, String email, UserRole role, Instant expiresAt) { }
}
