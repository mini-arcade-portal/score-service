package com.miniarcade.score_service.repository;

import com.miniarcade.score_service.entity.Difficulty;
import com.miniarcade.score_service.entity.Score;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ScoreRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private ScoreRepository scoreRepository;

    @Test
    void findsTopScoresByGameTypeOrderedDescending() {
        scoreRepository.save(newScore(1L, "alice", "snake", 100L));
        scoreRepository.save(newScore(2L, "bob", "snake", 300L));
        scoreRepository.save(newScore(3L, "carol", "snake", 200L));
        scoreRepository.save(newScore(4L, "dave", "tic-tac-toe", 999L));

        List<Score> top = scoreRepository.findByGameTypeOrderByScoreDesc("snake", PageRequest.of(0, 10));

        assertThat(top).hasSize(3);
        assertThat(top).extracting(Score::getScore).containsExactly(300L, 200L, 100L);
        assertThat(top).extracting(Score::getUsername).containsExactly("bob", "carol", "alice");
    }

    @Test
    void findsScoresByUserIdMostRecentFirst() {
        Score first = scoreRepository.save(newScore(1L, "alice", "snake", 50L));
        Score second = scoreRepository.save(newScore(1L, "alice", "snake", 75L));

        List<Score> history = scoreRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

        assertThat(history).extracting(Score::getId).containsExactly(second.getId(), first.getId());
    }

    private Score newScore(Long userId, String username, String gameType, Long score) {
        return Score.builder()
                .userId(userId)
                .username(username)
                .gameType(gameType)
                .score(score)
                .difficulty(Difficulty.MEDIUM)
                .build();
    }
}
