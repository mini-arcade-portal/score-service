package com.miniarcade.score_service.service;

import com.miniarcade.score_service.config.AuthenticatedUser;
import com.miniarcade.score_service.dto.ScoreResponse;
import com.miniarcade.score_service.dto.ScoreSubmitRequest;
import com.miniarcade.score_service.dto.StartSessionRequest;
import com.miniarcade.score_service.dto.StartSessionResponse;
import com.miniarcade.score_service.entity.GameSession;
import com.miniarcade.score_service.entity.Score;
import com.miniarcade.score_service.exception.ImplausibleScoreException;
import com.miniarcade.score_service.exception.InvalidSessionException;
import com.miniarcade.score_service.exception.NotFoundException;
import com.miniarcade.score_service.exception.SessionAlreadyUsedException;
import com.miniarcade.score_service.repository.GameSessionRepository;
import com.miniarcade.score_service.repository.ScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreService {

    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 10;

    private final ScoreRepository scoreRepository;
    private final GameSessionRepository gameSessionRepository;
    private final ScorePlausibilityService plausibilityService;

    @Value("${app.game-session.ttl-minutes:30}")
    private long sessionTtlMinutes;

    @Transactional
    public StartSessionResponse startSession(AuthenticatedUser user, StartSessionRequest req) {
        if (!plausibilityService.supports(req.gameType())) {
            throw new InvalidSessionException("Unsupported gameType: " + req.gameType());
        }

        Instant now = Instant.now();
        GameSession session = GameSession.builder()
                .id(UUID.randomUUID())
                .userId(user.userId())
                .gameType(req.gameType())
                .difficulty(req.difficulty())
                .createdAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(sessionTtlMinutes)))
                .used(false)
                .build();
        gameSessionRepository.save(session);

        return new StartSessionResponse(session.getId(), session.getExpiresAt());
    }

    @Transactional
    public ScoreResponse submit(AuthenticatedUser user, ScoreSubmitRequest req) {
        GameSession session = gameSessionRepository.findById(req.sessionId())
                .orElseThrow(() -> new NotFoundException("Session not found: " + req.sessionId()));

        if (!session.getUserId().equals(user.userId())) {
            throw new NotFoundException("Session not found: " + req.sessionId());
            // 404 instead of 403 on purpose — same as deleteOwnScore, don't leak existence
        }

        if (session.isUsed()) {
            throw new SessionAlreadyUsedException("Session already used: " + req.sessionId());
        }

        if (Instant.now().isAfter(session.getExpiresAt())) {
            throw new InvalidSessionException("Session expired: " + req.sessionId());
        }

        if (!session.getGameType().equals(req.gameType()) || session.getDifficulty() != req.difficulty()) {
            throw new InvalidSessionException("Session does not match submitted gameType/difficulty");
        }

        long elapsedMs = Duration.between(session.getCreatedAt(), Instant.now()).toMillis();
        long maxPlausible = plausibilityService.maxPlausibleScore(req.gameType(), req.difficulty(), elapsedMs);
        if (req.score() > maxPlausible) {
            log.warn("Implausible score rejected: userId={}, username={}, gameType={}, difficulty={}, " +
                            "claimedScore={}, maxScore={}, elapsed={}ms",
                    user.userId(), user.username(), req.gameType(), req.difficulty(),
                    req.score(), maxPlausible, elapsedMs);
            throw new ImplausibleScoreException("Score exceeds plausible maximum for elapsed time");
        }

        session.setUsed(true);
        gameSessionRepository.save(session);

        Score score = Score.builder()
                .userId(user.userId())
                .username(user.username())
                .gameType(req.gameType())
                .difficulty(req.difficulty())
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