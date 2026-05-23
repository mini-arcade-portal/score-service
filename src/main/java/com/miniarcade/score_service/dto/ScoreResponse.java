package com.miniarcade.score_service.dto;

import com.miniarcade.score_service.entity.Score;

import java.time.Instant;

public record ScoreResponse(
        Long id,
        Long userId,
        String username,
        String gameType,
        Long score,
        Instant createdAt
) {
    public static ScoreResponse from(Score s) {
        return new ScoreResponse(
                s.getId(),
                s.getUserId(),
                s.getUsername(),
                s.getGameType(),
                s.getScore(),
                s.getCreatedAt()
        );
    }
}