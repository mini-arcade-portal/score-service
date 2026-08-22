package com.miniarcade.score_service.service;

import com.miniarcade.score_service.config.AuthenticatedUser;
import com.miniarcade.score_service.dto.ScoreResponse;
import com.miniarcade.score_service.dto.ScoreSubmitRequest;
import com.miniarcade.score_service.dto.StartSessionRequest;
import com.miniarcade.score_service.dto.StartSessionResponse;
import com.miniarcade.score_service.entity.Difficulty;
import com.miniarcade.score_service.entity.GameSession;
import com.miniarcade.score_service.entity.Score;
import com.miniarcade.score_service.exception.ImplausibleScoreException;
import com.miniarcade.score_service.exception.InvalidSessionException;
import com.miniarcade.score_service.exception.NotFoundException;
import com.miniarcade.score_service.exception.SessionAlreadyUsedException;
import com.miniarcade.score_service.repository.GameSessionRepository;
import com.miniarcade.score_service.repository.ScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ScoreRepository scoreRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private ScorePlausibilityService plausibilityService;

    private ScoreService scoreService;

    private final AuthenticatedUser owner = new AuthenticatedUser(1L, "alice");
    private final AuthenticatedUser otherUser = new AuthenticatedUser(2L, "bob");

    @BeforeEach
    void setUp() {
        scoreService = new ScoreService(scoreRepository, gameSessionRepository, plausibilityService);
        ReflectionTestUtils.setField(scoreService, "sessionTtlMinutes", 30L);
    }

    private GameSession activeSession(UUID id, Long userId, String gameType, Difficulty difficulty) {
        Instant now = Instant.now();
        return GameSession.builder()
                .id(id)
                .userId(userId)
                .gameType(gameType)
                .difficulty(difficulty)
                .createdAt(now.minus(5, ChronoUnit.SECONDS))
                .expiresAt(now.plus(30, ChronoUnit.MINUTES))
                .used(false)
                .build();
    }

    @Test
    void submit_savesScoreAndMarksSessionUsed_whenSessionValidAndScorePlausible() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = activeSession(sessionId, 1L, "snake", Difficulty.HARD);
        ScoreSubmitRequest request = new ScoreSubmitRequest("snake", Difficulty.HARD, 150L, sessionId);

        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(plausibilityService.maxPlausibleScore(eq("snake"), eq(Difficulty.HARD), anyLong())).thenReturn(1000L);
        when(scoreRepository.save(any(Score.class))).thenAnswer(invocation -> {
            Score s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        ScoreResponse response = scoreService.submit(owner, request);

        ArgumentCaptor<Score> scoreCaptor = ArgumentCaptor.forClass(Score.class);
        verify(scoreRepository).save(scoreCaptor.capture());
        Score saved = scoreCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getGameType()).isEqualTo("snake");
        assertThat(saved.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(saved.getScore()).isEqualTo(150L);

        ArgumentCaptor<GameSession> sessionCaptor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().isUsed()).isTrue();

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    void submit_throwsNotFound_whenSessionDoesNotExist() {
        UUID sessionId = UUID.randomUUID();
        ScoreSubmitRequest request = new ScoreSubmitRequest("snake", Difficulty.HARD, 150L, sessionId);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoreService.submit(owner, request))
                .isInstanceOf(NotFoundException.class);

        verify(scoreRepository, never()).save(any());
    }

    @Test
    void submit_throwsNotFound_notForbidden_whenSessionBelongsToAnotherUser() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = activeSession(sessionId, 1L, "snake", Difficulty.HARD);
        ScoreSubmitRequest request = new ScoreSubmitRequest("snake", Difficulty.HARD, 150L, sessionId);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> scoreService.submit(otherUser, request))
                .isInstanceOf(NotFoundException.class);

        verify(scoreRepository, never()).save(any());
    }

    @Test
    void submit_throwsSessionAlreadyUsed_whenSessionAlreadyUsed() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = activeSession(sessionId, 1L, "snake", Difficulty.HARD);
        session.setUsed(true);
        ScoreSubmitRequest request = new ScoreSubmitRequest("snake", Difficulty.HARD, 150L, sessionId);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> scoreService.submit(owner, request))
                .isInstanceOf(SessionAlreadyUsedException.class);

        verify(scoreRepository, never()).save(any());
    }

    @Test
    void submit_throwsInvalidSession_whenSessionExpired() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = activeSession(sessionId, 1L, "snake", Difficulty.HARD);
        session.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        ScoreSubmitRequest request = new ScoreSubmitRequest("snake", Difficulty.HARD, 150L, sessionId);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> scoreService.submit(owner, request))
                .isInstanceOf(InvalidSessionException.class);

        verify(scoreRepository, never()).save(any());
    }

    @Test
    void submit_throwsInvalidSession_whenGameTypeMismatch() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = activeSession(sessionId, 1L, "snake", Difficulty.HARD);
        ScoreSubmitRequest request = new ScoreSubmitRequest("flappybird", Difficulty.HARD, 150L, sessionId);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> scoreService.submit(owner, request))
                .isInstanceOf(InvalidSessionException.class);

        verify(scoreRepository, never()).save(any());
    }

    @Test
    void submit_throwsInvalidSession_whenDifficultyMismatch() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = activeSession(sessionId, 1L, "snake", Difficulty.HARD);
        ScoreSubmitRequest request = new ScoreSubmitRequest("snake", Difficulty.EASY, 150L, sessionId);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> scoreService.submit(owner, request))
                .isInstanceOf(InvalidSessionException.class);

        verify(scoreRepository, never()).save(any());
    }

    @Test
    void submit_throwsImplausibleScore_whenScoreExceedsMaxPlausible() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = activeSession(sessionId, 1L, "snake", Difficulty.HARD);
        ScoreSubmitRequest request = new ScoreSubmitRequest("snake", Difficulty.HARD, 999_999L, sessionId);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(plausibilityService.maxPlausibleScore(eq("snake"), eq(Difficulty.HARD), anyLong())).thenReturn(50L);

        assertThatThrownBy(() -> scoreService.submit(owner, request))
                .isInstanceOf(ImplausibleScoreException.class);

        verify(scoreRepository, never()).save(any());
        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    void startSession_createsSessionWithExpectedFields() {
        StartSessionRequest request = new StartSessionRequest("snake", Difficulty.HARD);
        when(plausibilityService.supports("snake")).thenReturn(true);
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        StartSessionResponse response = scoreService.startSession(owner, request);

        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(captor.capture());
        GameSession saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getGameType()).isEqualTo("snake");
        assertThat(saved.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(saved.getCreatedAt());

        assertThat(response.sessionId()).isEqualTo(saved.getId());
        assertThat(response.expiresAt()).isEqualTo(saved.getExpiresAt());
    }

    @Test
    void startSession_throwsInvalidSession_whenGameTypeUnsupported() {
        StartSessionRequest request = new StartSessionRequest("unknown-game", Difficulty.EASY);
        when(plausibilityService.supports("unknown-game")).thenReturn(false);

        assertThatThrownBy(() -> scoreService.startSession(owner, request))
                .isInstanceOf(InvalidSessionException.class);

        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    void topScores_usesDefaultLimitOfTen_whenLimitIsNull() {
        when(scoreRepository.findByGameTypeOrderByScoreDesc(eq("snake"), any(Pageable.class)))
                .thenReturn(List.of());

        scoreService.topScores("snake", null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scoreRepository).findByGameTypeOrderByScoreDesc(eq("snake"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 10));
    }

    @Test
    void topScores_clampsLimitToMaxOfHundred_whenLimitExceedsMax() {
        when(scoreRepository.findByGameTypeOrderByScoreDesc(eq("snake"), any(Pageable.class)))
                .thenReturn(List.of());

        scoreService.topScores("snake", null, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scoreRepository).findByGameTypeOrderByScoreDesc(eq("snake"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 100));
    }

    @Test
    void topScores_usesRequestedLimit_whenWithinBounds() {
        when(scoreRepository.findByGameTypeOrderByScoreDesc(eq("snake"), any(Pageable.class)))
                .thenReturn(List.of());

        scoreService.topScores("snake", null, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scoreRepository).findByGameTypeOrderByScoreDesc(eq("snake"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 5));
    }

    @Test
    void topScores_fallsBackToDefaultLimit_whenLimitIsZeroOrNegative() {
        when(scoreRepository.findByGameTypeOrderByScoreDesc(eq("snake"), any(Pageable.class)))
                .thenReturn(List.of());

        scoreService.topScores("snake", null, -1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scoreRepository).findByGameTypeOrderByScoreDesc(eq("snake"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 10));
    }

    @Test
    void myScores_usesUserIdFromAuthenticatedUser_andClampsLimit() {
        when(scoreRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of());

        scoreService.myScores(owner, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scoreRepository).findByUserIdOrderByCreatedAtDesc(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 100));
    }

    @Test
    void deleteOwnScore_deletesScore_whenCallerIsOwner() {
        Score score = Score.builder().id(5L).userId(1L).username("alice")
                .gameType("snake").difficulty(Difficulty.EASY).score(10L).build();
        when(scoreRepository.findById(5L)).thenReturn(Optional.of(score));

        scoreService.deleteOwnScore(owner, 5L);

        verify(scoreRepository).delete(score);
    }

    @Test
    void deleteOwnScore_throwsNotFound_whenScoreDoesNotExist() {
        when(scoreRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoreService.deleteOwnScore(owner, 999L))
                .isInstanceOf(NotFoundException.class);

        verify(scoreRepository, never()).delete(any());
    }

    @Test
    void deleteOwnScore_throwsNotFound_notForbidden_whenCallerIsNotOwner() {
        Score score = Score.builder().id(5L).userId(1L).username("alice")
                .gameType("snake").difficulty(Difficulty.EASY).score(10L).build();
        when(scoreRepository.findById(5L)).thenReturn(Optional.of(score));

        assertThatThrownBy(() -> scoreService.deleteOwnScore(otherUser, 5L))
                .isInstanceOf(NotFoundException.class);

        verify(scoreRepository, never()).delete(any());
    }
}
