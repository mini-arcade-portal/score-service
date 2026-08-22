package com.miniarcade.score_service.service;

import com.miniarcade.score_service.entity.Difficulty;
import com.miniarcade.score_service.exception.InvalidSessionException;
import com.miniarcade.score_service.service.plausibility.FlappyBirdPlausibilityStrategy;
import com.miniarcade.score_service.service.plausibility.PlausibilityStrategy;
import com.miniarcade.score_service.service.plausibility.SnakePlausibilityStrategy;
import com.miniarcade.score_service.service.plausibility.TicTacToePlausibilityStrategy;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ScorePlausibilityService {

    private final Map<String, PlausibilityStrategy> strategies;

    public ScorePlausibilityService(
            SnakePlausibilityStrategy snake,
            FlappyBirdPlausibilityStrategy flappyBird,
            TicTacToePlausibilityStrategy ticTacToe
    ) {
        this.strategies = Map.of(
                "snake", snake,
                "flappybird", flappyBird,
                "tictactoe", ticTacToe
        );
    }

    public boolean supports(String gameType) {
        return strategies.containsKey(gameType);
    }

    public long maxPlausibleScore(String gameType, Difficulty difficulty, long elapsedMs) {
        PlausibilityStrategy strategy = strategies.get(gameType);
        if (strategy == null) {
            throw new InvalidSessionException("No plausibility strategy for gameType: " + gameType);
        }
        return strategy.maxScore(difficulty, elapsedMs);
    }
}
