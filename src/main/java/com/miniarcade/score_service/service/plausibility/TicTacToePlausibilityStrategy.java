package com.miniarcade.score_service.service.plausibility;

import com.miniarcade.score_service.entity.Difficulty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mirrors frontend/src/games/tictactoe/types.ts (DIFFICULTIES multipliers,
 * calculateFinalScore) and useTicTacToeGame.ts (AI_THINK_MS=650, a real
 * setTimeout floor before the AI moves; starter alternates X/O each round,
 * starting with X on round 1 — reset()/nextRound()).
 *
 * PLAYER_MOVE_MIN_MS is a heuristic, not code-derived (unlike AI_THINK_MS) —
 * a conservative floor on human click speed, documented as a limitation.
 * Fastest streak-extending path on every difficulty is a win (5-6 plies),
 * since a draw (9 plies) is always slower — draws are not modeled separately.
 * No hard cap — the game is endless by design, bounded only by the session TTL.
 */
@Component
public class TicTacToePlausibilityStrategy implements PlausibilityStrategy {

    private static final int AI_THINK_MS = 650;
    private static final int PLAYER_MOVE_MIN_MS = 150;
    private static final int MOVES_PER_ROUND = 3; // fastest win = 3 X-marks

    private static final Map<Difficulty, Integer> MULTIPLIERS = Map.of(
            Difficulty.EASY, 1,
            Difficulty.MEDIUM, 2,
            Difficulty.HARD, 3
    );

    @Override
    public long maxScore(Difficulty difficulty, long elapsedMs) {
        long remaining = elapsedMs;
        int round = 1;
        int streak = 0;

        while (true) {
            boolean starterIsX = (round % 2 == 1);
            int aiMoves = starterIsX ? 2 : 3;
            long roundMinMs = (long) aiMoves * AI_THINK_MS + (long) MOVES_PER_ROUND * PLAYER_MOVE_MIN_MS;

            if (remaining < roundMinMs) break;
            remaining -= roundMinMs;
            streak++;
            round++;
        }

        return (long) streak * MULTIPLIERS.get(difficulty);
    }
}
