package com.miniarcade.score_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "test-secret-key-at-least-64-bytes-long-for-hs512-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        jwtService.init();
    }

    private String issueToken(Map<String, Object> claims, String subject, Date expiry, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    @Test
    void parse_returnsClaims_forValidToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 42L);
        claims.put("role", "USER");
        String token = issueToken(claims, "alice", new Date(System.currentTimeMillis() + 60_000), TEST_SECRET);

        Claims parsed = jwtService.parse(token);

        assertThat(parsed.getSubject()).isEqualTo("alice");
        assertThat(parsed.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void parse_throwsJwtException_forExpiredToken() {
        String token = issueToken(Map.of("userId", 1), "alice", new Date(System.currentTimeMillis() - 1000), TEST_SECRET);

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void parse_throwsJwtException_forSignatureMismatch() {
        String differentSecret = "a-completely-different-secret-key-that-is-also-at-least-64-bytes-long-xx";
        String token = issueToken(Map.of("userId", 1), "alice", new Date(System.currentTimeMillis() + 60_000), differentSecret);

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void parse_throwsJwtException_forMalformedToken() {
        assertThatThrownBy(() -> jwtService.parse("not-a-real-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void extractUserId_handlesIntegerClaim() {
        String token = issueToken(Map.of("userId", 42), "alice", new Date(System.currentTimeMillis() + 60_000), TEST_SECRET);
        Claims claims = jwtService.parse(token);

        assertThat(jwtService.extractUserId(claims)).isEqualTo(42L);
    }

    @Test
    void extractUserId_handlesLongClaim() {
        String token = issueToken(Map.of("userId", 42L), "alice", new Date(System.currentTimeMillis() + 60_000), TEST_SECRET);
        Claims claims = jwtService.parse(token);

        assertThat(jwtService.extractUserId(claims)).isEqualTo(42L);
    }

    @Test
    void extractUserId_throwsIllegalArgument_whenClaimMissing() {
        String token = issueToken(Map.of(), "alice", new Date(System.currentTimeMillis() + 60_000), TEST_SECRET);
        Claims claims = jwtService.parse(token);

        assertThatThrownBy(() -> jwtService.extractUserId(claims)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractUsername_returnsSubject() {
        String token = issueToken(Map.of("userId", 1), "alice", new Date(System.currentTimeMillis() + 60_000), TEST_SECRET);
        Claims claims = jwtService.parse(token);

        assertThat(jwtService.extractUsername(claims)).isEqualTo("alice");
    }

    @Test
    void extractRole_returnsNull_whenRoleClaimAbsent() {
        String token = issueToken(Map.of("userId", 1), "alice", new Date(System.currentTimeMillis() + 60_000), TEST_SECRET);
        Claims claims = jwtService.parse(token);

        assertThat(jwtService.extractRole(claims)).isNull();
    }

    @Test
    void extractRole_returnsRole_whenPresent() {
        String token = issueToken(Map.of("userId", 1, "role", "ADMIN"), "alice", new Date(System.currentTimeMillis() + 60_000), TEST_SECRET);
        Claims claims = jwtService.parse(token);

        assertThat(jwtService.extractRole(claims)).isEqualTo("ADMIN");
    }
}
