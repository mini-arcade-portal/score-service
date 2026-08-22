package com.miniarcade.score_service.service.plausibility;

import com.miniarcade.score_service.entity.Difficulty;

public interface PlausibilityStrategy {

    /** Maximum score achievable in {@code elapsedMs} on the given difficulty. */
    long maxScore(Difficulty difficulty, long elapsedMs);
}
