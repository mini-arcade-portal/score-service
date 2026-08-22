package com.miniarcade.score_service.service.plausibility;

import com.miniarcade.score_service.entity.Difficulty;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlappyBirdPlausibilityStrategyTest {

    private final FlappyBirdPlausibilityStrategy strategy = new FlappyBirdPlausibilityStrategy();

    @Test
    void maxScore_isZero_whenNoTimeHasElapsed() {
        assertThat(strategy.maxScore(Difficulty.EASY, 0)).isZero();
        assertThat(strategy.maxScore(Difficulty.MEDIUM, 0)).isZero();
        assertThat(strategy.maxScore(Difficulty.HARD, 0)).isZero();
    }

    @Test
    void maxScore_easy_crossesFirstPipeExactlyAtBufferBoundary() {
        // First pipe needs 260px spawn-delay + 388px travel buffer = 648px,
        // at scrollSpeed=140px/s that's 4628.57ms -> boundary at 4629ms.
        assertThat(strategy.maxScore(Difficulty.EASY, 4628)).isZero();
        assertThat(strategy.maxScore(Difficulty.EASY, 4629)).isEqualTo(1);
    }

    @Test
    void maxScore_medium_crossesFirstPipeExactlyAtBufferBoundary() {
        // 648px at scrollSpeed=190px/s = 3410.53ms -> boundary at 3411ms, multiplier 2
        assertThat(strategy.maxScore(Difficulty.MEDIUM, 3410)).isZero();
        assertThat(strategy.maxScore(Difficulty.MEDIUM, 3411)).isEqualTo(2);
    }

    @Test
    void maxScore_hasNoHardCap_growsUnboundedWithElapsedTime() {
        long shortRun = strategy.maxScore(Difficulty.HARD, 60_000);
        long longRun = strategy.maxScore(Difficulty.HARD, 600_000);

        assertThat(longRun).isGreaterThan(shortRun);
    }

    @Test
    void maxScore_isMonotonicallyNonDecreasing_asElapsedTimeGrows() {
        for (Difficulty d : Difficulty.values()) {
            long previous = 0;
            for (long elapsedMs = 0; elapsedMs <= 30_000; elapsedMs += 250) {
                long current = strategy.maxScore(d, elapsedMs);
                assertThat(current).as("difficulty=%s elapsedMs=%d", d, elapsedMs).isGreaterThanOrEqualTo(previous);
                previous = current;
            }
        }
    }
}
