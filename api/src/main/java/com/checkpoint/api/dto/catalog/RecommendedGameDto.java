package com.checkpoint.api.dto.catalog;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO returned by the recommendation endpoint. Carries the same minimal game
 * fields as {@link GameCardDto} plus a {@code reason} string explaining why
 * the game was recommended (e.g. "Trending this week", "Because you liked …").
 */
public record RecommendedGameDto(
        UUID id,
        String title,
        String coverUrl,
        LocalDate releaseDate,
        Double averageRating,
        String reason
) {
    public RecommendedGameDto(UUID id, String title, String coverUrl, LocalDate releaseDate,
                              Double averageRating, String reason) {
        this.id = id;
        this.title = title;
        this.coverUrl = coverUrl;
        this.releaseDate = releaseDate;
        this.averageRating = averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : null;
        this.reason = reason;
    }
}
