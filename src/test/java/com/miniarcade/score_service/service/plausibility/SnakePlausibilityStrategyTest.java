package com.miniarcade.score_service.service.plausibility;

import com.miniarcade.score_service.entity.Difficulty;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnakePlausibilityStrategyTest {

    private final SnakePlausibilityStrategy strategy = new SnakePlausibilityStrategy();

    @Test
    void maxScore_isZero_whenNoTimeHasElapsed() {
        assertThat(strategy.maxScore(Difficulty.EASY, 0)).isZero();
        assertThat(strategy.maxScore(Difficulty.MEDIUM, 0)).isZero();
        assertThat(strategy.maxScore(Difficulty.HARD, 0)).isZero();
    }

    @Test
    void maxScore_easy_crossesFirstAppleExactlyAtStartSpeedBoundary() {
        // EASY startSpeed=180ms, no speedup, first apple = floor((10+0)*1.0) = 10
        assertThat(strategy.maxScore(Difficulty.EASY, 179)).isZero();
        assertThat(strategy.maxScore(Difficulty.EASY, 180)).isEqualTo(10);
    }

    @Test
    void maxScore_hard_crossesFirstAppleExactlyAtStartSpeedBoundary() {
        // HARD startSpeed=90ms, first apple = floor((10+0)*1.6) = 16
        assertThat(strategy.maxScore(Difficulty.HARD, 89)).isZero();
        assertThat(strategy.maxScore(Difficulty.HARD, 90)).isEqualTo(16);
    }

    @Test
    void maxScore_isMonotonicallyNonDecreasing_asElapsedTimeGrows() {
        for (Difficulty d : Difficulty.values()) {
            long previous = 0;
            for (long elapsedMs = 0; elapsedMs <= 60_000; elapsedMs += 500) {
                long current = strategy.maxScore(d, elapsedMs);
                assertThat(current).as("difficulty=%s elapsedMs=%d", d, elapsedMs).isGreaterThanOrEqualTo(previous);
                previous = current;
            }
        }
    }

    @Test
    void maxScore_saturatesAtHardCap_whenElapsedTimeIsVeryLarge() {
        // 222 apples is the board's physical maximum (15x15 - starting length 3) —
        // beyond the time needed to eat all of them, score can't grow further.
        for (Difficulty d : Difficulty.values()) {
            long plateau = strategy.maxScore(d, 10_000_000);
            long stillPlateau = strategy.maxScore(d, 20_000_000);
            assertThat(stillPlateau).as("difficulty=%s", d).isEqualTo(plateau);
        }
    }

    @Test
    void maxScore_harderDifficultyReachesHigherScoreOverSameElapsedTime() {
        long elapsed = 30_000;
        long easy = strategy.maxScore(Difficulty.EASY, elapsed);
        long medium = strategy.maxScore(Difficulty.MEDIUM, elapsed);
        long hard = strategy.maxScore(Difficulty.HARD, elapsed);

        assertThat(medium).isGreaterThan(easy);
        assertThat(hard).isGreaterThan(medium);
    }
}
