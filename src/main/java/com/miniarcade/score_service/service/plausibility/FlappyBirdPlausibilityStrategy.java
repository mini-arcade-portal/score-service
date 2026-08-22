package com.miniarcade.score_service.service.plausibility;

import com.miniarcade.score_service.entity.Difficulty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mirrors frontend/src/games/flappybird/types.ts (DIFFICULTIES, PIPE_SPACING,
 * CANVAS_WIDTH, BIRD_X, BIRD_RADIUS, PIPE_WIDTH) and the pass-detection condition
 * in useFlappyBirdGame.ts (`p.x + PIPE_WIDTH < BIRD_X - BIRD_RADIUS`).
 *
 * The first pipe spawns only after PIPE_SPACING px of scroll distance has
 * accumulated, then must travel BUFFER_PX further before it's counted as passed.
 * No hard cap — the game is endless by design, bounded only by the session TTL.
 */
@Component
public class FlappyBirdPlausibilityStrategy implements PlausibilityStrategy {

    private static final int CANVAS_WIDTH = 400;
    private static final int BIRD_X = 90;
    private static final int BIRD_RADIUS = 14;
    private static final int PIPE_WIDTH = 64;
    private static final int PIPE_SPACING = 260;
    private static final int BUFFER_PX = CANVAS_WIDTH - (BIRD_X - BIRD_RADIUS - PIPE_WIDTH); // 388

    private record Config(int scrollSpeed, int multiplier) {}

    private static final Map<Difficulty, Config> CONFIGS = Map.of(
            Difficulty.EASY, new Config(140, 1),
            Difficulty.MEDIUM, new Config(190, 2),
            Difficulty.HARD, new Config(240, 3)
    );

    @Override
    public long maxScore(Difficulty difficulty, long elapsedMs) {
        Config cfg = CONFIGS.get(difficulty);
        double elapsedPx = elapsedMs / 1000.0 * cfg.scrollSpeed();
        if (elapsedPx <= BUFFER_PX) return 0;

        long maxPipes = (long) Math.floor((elapsedPx - BUFFER_PX) / PIPE_SPACING);
        return maxPipes * cfg.multiplier();
    }
}
