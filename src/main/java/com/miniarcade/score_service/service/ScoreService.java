package com.miniarcade.score_service.service;

import com.miniarcade.score_service.config.AuthenticatedUser;
import com.miniarcade.score_service.dto.ScoreResponse;
import com.miniarcade.score_service.dto.ScoreSubmitRequest;
import com.miniarcade.score_service.entity.Score;
import com.miniarcade.score_service.exception.NotFoundException;
import com.miniarcade.score_service.repository.ScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 10;

    private final ScoreRepository scoreRepository;

    @Transactional
    public ScoreResponse submit(AuthenticatedUser user, ScoreSubmitRequest req) {
        Score score = Score.builder()
                .userId(user.userId())
                .username(user.username())
                .gameType(req.gameType())
                .score(req.score())
                .build();
        return ScoreResponse.from(scoreRepository.save(score));
    }

    @Transactional(readOnly = true)
    public List<ScoreResponse> topScores(String gameType, Integer limit) {
        int safeLimit = clampLimit(limit);
        return scoreRepository
                .findByGameTypeOrderByScoreDesc(gameType, PageRequest.of(0, safeLimit))
                .stream()
                .map(ScoreResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScoreResponse> myScores(AuthenticatedUser user, Integer limit) {
        int safeLimit = clampLimit(limit);
        return scoreRepository
                .findByUserIdOrderByCreatedAtDesc(user.userId(), PageRequest.of(0, safeLimit))
                .stream()
                .map(ScoreResponse::from)
                .toList();
    }

    @Transactional
    public void deleteOwnScore(AuthenticatedUser user, Long id) {
        Score score = scoreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Score not found: " + id));

        if (!score.getUserId().equals(user.userId())) {
            throw new NotFoundException("Score not found: " + id);
            // 404 instead of 403 on purpose — don't leak existence to other users
        }
        scoreRepository.delete(score);
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }
}