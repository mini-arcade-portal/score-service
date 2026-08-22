package com.miniarcade.score_service.service.plausibility;

import com.miniarcade.score_service.entity.Difficulty;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicTacToePlausibilityStrategyTest {

    private final TicTacToePlausibilityStrategy strategy = new TicTacToePlausibilityStrategy();

    // Round 1: X starts, aiMoves=2, 3 player moves -> 2*650 + 3*150 = 1750ms
    private static final long ROUND_1_MIN_MS = 1750;
    // Round 2: O starts, aiMoves=3, 3 player moves -> 3*650 + 3*150 = 2400ms
    private static final long ROUND_2_MIN_MS = 2400;

    @Test
    void maxScore_isZero_whenNoTimeHasElapsed() {
        assertThat(strategy.maxScore(Difficulty.EASY, 0)).isZero();
    }

    @Test
    void maxScore_crossesFirstRoundExactlyAtBoundary() {
        assertThat(strategy.maxScore(Difficulty.EASY, ROUND_1_MIN_MS - 1)).isZero();
        assertThat(strategy.maxScore(Difficulty.EASY, ROUND_1_MIN_MS)).isEqualTo(1);
    }

    @Test
    void maxScore_crossesSecondRoundExactlyAtBoundary() {
        long twoRounds = ROUND_1_MIN_MS + ROUND_2_MIN_MS;
        assertThat(strategy.maxScore(Difficulty.EASY, twoRounds - 1)).isEqualTo(1);
        assertThat(strategy.maxScore(Difficulty.EASY, twoRounds)).isEqualTo(2);
    }

    @Test
    void maxScore_scalesLinearlyWithDifficultyMultiplier() {
        long easy = strategy.maxScore(Difficulty.EASY, ROUND_1_MIN_MS);
        long medium = strategy.maxScore(Difficulty.MEDIUM, ROUND_1_MIN_MS);
        long hard = strategy.maxScore(Difficulty.HARD, ROUND_1_MIN_MS);

        assertThat(easy).isEqualTo(1);
        assertThat(medium).isEqualTo(2);
        assertThat(hard).isEqualTo(3);
    }

    @Test
    void maxScore_hasNoHardCap_growsUnboundedWithElapsedTime() {
        long shortRun = strategy.maxScore(Difficulty.HARD, 60_000);
        long longRun = strategy.maxScore(Difficulty.HARD, 600_000);

        assertThat(longRun).isGreaterThan(shortRun);
    }
}
