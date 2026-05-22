package com.checkpoint.api.dto.admin;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request body for fully updating a video game via the admin API.
 * IGDB / Steam external identifiers are intentionally not exposed: those
 * stay read-only to keep the import pipeline idempotent.
 */
public record UpdateGameRequestDto(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        String description,

        @Size(max = 1024, message = "coverUrl must be at most 1024 characters")
        String coverUrl,

        @Size(max = 1024, message = "artworkUrl must be at most 1024 characters")
        String artworkUrl,

        @Size(max = 64, message = "trailerYoutubeId must be at most 64 characters")
        String trailerYoutubeId,

        @PositiveOrZero(message = "timeToBeatNormally must be zero or positive")
        Long timeToBeatNormally,

        @PositiveOrZero(message = "timeToBeatHastily must be zero or positive")
        Long timeToBeatHastily,

        @PositiveOrZero(message = "timeToBeatCompletely must be zero or positive")
        Long timeToBeatCompletely,

        LocalDate releaseDate,

        Set<UUID> genreIds,
        Set<UUID> platformIds,
        Set<UUID> companyIds
) {}
