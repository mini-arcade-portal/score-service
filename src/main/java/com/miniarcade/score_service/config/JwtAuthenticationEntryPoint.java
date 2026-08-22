package com.miniarcade.score_service.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Without an explicit entry point, Spring Security falls back to
 * Http403ForbiddenEntryPoint for unauthenticated requests (no httpBasic()/formLogin()
 * configured), returning 403 instead of 401 for missing/expired/invalid tokens.
 * This restores the 401 that JwtAuthFilter's contract already assumes.
 *
 * JSON is written by hand (no ObjectMapper dependency) since this runs outside the
 * normal MVC message-converter pipeline, at the filter level.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws java.io.IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String body = """
                {"timestamp":"%s","status":401,"message":"Authentication required or token expired"}"""
                .formatted(Instant.now());

        response.getWriter().write(body);
    }
}
