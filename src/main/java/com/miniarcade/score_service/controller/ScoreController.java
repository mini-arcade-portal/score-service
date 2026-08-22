package com.miniarcade.score_service.controller;

import com.miniarcade.score_service.config.AuthenticatedUser;
import com.miniarcade.score_service.dto.ScoreResponse;
import com.miniarcade.score_service.dto.ScoreSubmitRequest;
import com.miniarcade.score_service.dto.StartSessionRequest;
import com.miniarcade.score_service.dto.StartSessionResponse;
import com.miniarcade.score_service.service.ScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    /** Start a single-use game session (auth required). */
    @PostMapping("/sessions")
    public ResponseEntity<StartSessionResponse> startSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody StartSessionRequest req
    ) {
        StartSessionResponse session = scoreService.startSession(user, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /** Submit a new score (auth required). */
    @PostMapping
    public ResponseEntity<ScoreResponse> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ScoreSubmitRequest req
    ) {
        ScoreResponse saved = scoreService.submit(user, req);
        return ResponseEntity
                .created(URI.create("/api/scores/" + saved.id()))
                .body(saved);
    }

    /** Top scores for a game (public). */
    @GetMapping
    public List<ScoreResponse> topScores(
            @RequestParam String gameType,
            @RequestParam(required = false) Integer limit
    ) {
        return scoreService.topScores(gameType, limit);
    }

    /** Caller's own scores (auth required). */
    @GetMapping("/me")
    public List<ScoreResponse> myScores(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Integer limit
    ) {
        return scoreService.myScores(user, limit);
    }

    /** Delete the caller's own score (auth required). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        scoreService.deleteOwnScore(user, id);
    }
}