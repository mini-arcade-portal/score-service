package com.miniarcade.score_service.service.plausibility;

import com.miniarcade.score_service.entity.Difficulty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mirrors frontend/src/games/snake/types.ts (DIFFICULTIES, calculateAppleScore)
 * and useSnakeGame.ts (tick/speedup logic). currentSpeed is literally ms-per-tick
 * (setTimeout(tick, currentSpeed)), so score-per-apple and time-per-apple change
 * together as the snake speeds up — simulated apple-by-apple rather than closed-form.
 */
@Component
public class SnakePlausibilityStrategy implements PlausibilityStrategy {

    private static final int BOARD_SIZE = 15;
    private static final int START_LENGTH = 3;
    private static final int MAX_APPLES = BOARD_SIZE * BOARD_SIZE - START_LENGTH; // 222
    private static final int BASE_APPLE_SCORE = 10;

    private record Config(int startSpeed, int speedUpEvery, int speedUpStep, int minSpeed, double multiplier) {}

    private static final Map<Difficulty, Config> CONFIGS = Map.of(
            Difficulty.EASY, new Config(180, Integer.MAX_VALUE, 0, 180, 1.0),
            Difficulty.MEDIUM, new Config(130, 5, 5, 70, 1.3),
            Difficulty.HARD, new Config(90, 3, 5, 50, 1.6)
    );

    @Override
    public long maxScore(Difficulty difficulty, long elapsedMs) {
        Config cfg = CONFIGS.get(difficulty);
        long remainingMs = elapsedMs;
        int currentSpeed = cfg.startSpeed();
        long total = 0;

        for (int apple = 1; apple <= MAX_APPLES; apple++) {
            if (remainingMs < currentSpeed) break;
            remainingMs -= currentSpeed;

            int speedBonus = (cfg.startSpeed() - currentSpeed) / 10;
            total += (long) Math.floor((BASE_APPLE_SCORE + speedBonus) * cfg.multiplier());

            if (apple % cfg.speedUpEvery() == 0) {
                currentSpeed = Math.max(cfg.minSpeed(), currentSpeed - cfg.speedUpStep());
            }
        }

        return total;
    }
}
