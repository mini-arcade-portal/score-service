package com.miniarcade.score_service.dto;

import java.time.Instant;
import java.util.UUID;

public record StartSessionResponse(UUID sessionId, Instant expiresAt) {}
