package com.miniarcade.score_service.dto;

import com.miniarcade.score_service.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ScoreSubmitRequest(

        @NotBlank
        @Size(max = 32)
        String gameType,

        @NotNull
        Difficulty difficulty,

        @NotNull
        @PositiveOrZero
        Long score
) {}