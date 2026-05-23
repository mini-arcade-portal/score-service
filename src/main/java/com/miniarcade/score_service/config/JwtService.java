package com.miniarcade.score_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    void init() {
        // The same secret must be configured in the auth-service.
        // Must be at least 32 bytes for HS256.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses and validates the token. Throws JwtException (or a subclass)
     * if invalid / expired / signature mismatch.
     */
    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    public Long extractUserId(Claims claims) {
        Object sub = claims.getSubject();
        if (sub == null) {
            throw new IllegalArgumentException("Token missing subject (userId)");
        }
        return Long.parseLong(sub.toString());
    }

    public String extractUsername(Claims claims) {
        Object name = claims.get("username");
        return name != null ? name.toString() : null;
    }
}