package com.miniarcade.score_service.service;

import com.miniarcade.score_service.config.AuthenticatedUser;
import com.miniarcade.score_service.dto.ScoreResponse;
import com.miniarcade.score_service.dto.ScoreSubmitRequest;
import com.miniarcade.score_service.entity.Difficulty;
import com.miniarcade.score_service.entity.Score;
import com.miniarcade.score_service.exception.NotFoundException;
import com.miniarcade.score_service.repository.ScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ScoreRepository scoreRepository;

    private ScoreService scoreService;

    private final AuthenticatedUser owner = new AuthenticatedUser(1L, "alice");
    private final AuthenticatedUser otherUser = new AuthenticatedUser(2L, "bob");

    @BeforeEach
    void setUp() {
        scoreService = new ScoreService(scoreRepository);
    }

    @Test
    void submit_savesScoreWithFieldsFromAuthenticatedUserAndRequest() {
        ScoreSubmitRequest request = new ScoreSubmitRequest("snake", Difficulty.HARD, 150L);
        when(scoreRepository.save(any(Score.class))).thenAnswer(invocation -> {
            Score s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        ScoreResponse response = scoreService.submit(owner, request);

        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        verify(scoreRepository).save(captor.capture());
        Score saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getGameType()).isEqualTo("snake");
        assertThat(saved.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(saved.getScore()).isEqualTo(150L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    void topScores_usesDefaultLimitOfTen_whenLimitIsNull() {
        when(scoreRepository.findByGameTypeOrderByScoreDesc(eq("snake"), any(Pageable.class)))
                .thenReturn(List.of());

        scoreService.topScores("snake", null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scoreRepository).findByGameTypeOrderByScoreDesc(eq("snake"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 10));
    }

    @Test
    void topScores_clampsLimitToMaxOfHundred_whenLimitExceedsMax() {
        when(scoreRepository.findByGameTypeOrderByScoreDesc(eq("snake"), any(Pageable.class)))
                .thenReturn(List.of());

        scoreService.topScores("snake", 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scoreRepository).findByGameTypeOrderByScoreDesc(eq("snake"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 100));
    }

    @Test
    void topScores_usesRequestedLimit_whenWithinBounds() {
        when(scoreRepository.findByGameTypeOrderByScoreDesc(eq("snake"), any(Pageable.class)))
                .thenReturn(List.of());

        scoreService.topScores("snake", 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(scoreRepository).findByGameTypeOrderByScoreDesc(eq("snake"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 5));
    }

    @Test
    void topScores_fallsBackToDefaultLimit_whenLimitIsZeroOrNegative() {
        when(scoreRepository.findByGameTypeOrderByScoreDesc(eq("snake"), any(Pageable.class)))
                .thenReturn(List.of());

        scoreService.topScores("snake", -1);

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
