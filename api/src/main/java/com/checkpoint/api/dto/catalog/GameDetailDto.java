package com.checkpoint.api.dto.catalog;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for detailed game information.
 * Contains all information needed for a game detail page.
 */
public record GameDetailDto(
        UUID id,
        String title,
        String description,
        String coverUrl,
        String artworkUrl,
        String trailerYoutubeId,
        Long timeToBeatNormally,
        Long timeToBeatHastily,
        Long timeToBeatCompletely,
        LocalDate releaseDate,
        Double averageRating,
        Long ratingCount,
        List<GenreDto> genres,
        List<PlatformDto> platforms,
        List<CompanyDto> companies
) {
    /**
     * Nested DTO for genre information.
     */
    public record GenreDto(UUID id, String name) {}

    /**
     * Nested DTO for platform information.
     */
    public record PlatformDto(UUID id, String name) {}

    /**
     * Nested DTO for company information.
     */
    public record CompanyDto(UUID id, String name) {}
}
