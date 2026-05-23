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

    @Value("${app.jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    void init() {
        // Must match auth-service's app.jwt.secret exactly.
        // At least 64 bytes for HS512 (Keys.hmacShaKeyFor picks the alg by key length).
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

    /** Numeric user id from the "userId" claim (set by auth-service). */
    public Long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Token missing userId claim");
        }
        // Jackson may deserialize as Integer or Long depending on size.
        if (userId instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(userId.toString());
    }

    /** Username is in the "sub" (subject) claim. */
    public String extractUsername(Claims claims) {
        return claims.getSubject();
    }

    /** Role string from the "role" claim ("USER" or "ADMIN"). May be null. */
    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }
}