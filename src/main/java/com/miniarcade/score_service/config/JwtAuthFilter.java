package com.miniarcade.score_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER.length());

        try {
            Claims claims = jwtService.parse(token);
            Long userId = jwtService.extractUserId(claims);
            String username = jwtService.extractUsername(claims);

            AuthenticatedUser principal = new AuthenticatedUser(userId, username);

            var auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of() // no roles needed for this service
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException | IllegalArgumentException ex) {
            // Invalid/expired/malformed token — leave context unauthenticated.
            // Spring Security will return 401 on protected endpoints.
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}