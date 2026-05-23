package com.miniarcade.score_service.config;

/**
 * The authenticated principal placed into the SecurityContext after JWT validation.
 * Accessible from controllers via @AuthenticationPrincipal.
 */
public record AuthenticatedUser(Long userId, String username) {}