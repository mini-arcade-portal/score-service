package com.miniarcade.score_service.repository;

import com.miniarcade.score_service.entity.Difficulty;
import com.miniarcade.score_service.entity.Score;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {

    List<Score> findByGameTypeOrderByScoreDesc(String gameType, Pageable pageable);

    List<Score> findByGameTypeAndDifficultyOrderByScoreDesc(String gameType, Difficulty difficulty, Pageable pageable);

    List<Score> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}