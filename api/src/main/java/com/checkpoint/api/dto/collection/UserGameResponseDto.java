package com.checkpoint.api.dto.collection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.checkpoint.api.enums.PlayStatus;

/**
 * Response DTO for a game in the user's collection.
 *
 * @param id          the user-game association ID
 * @param videoGameId the video game ID
 * @param title       the video game title
 * @param coverUrl    the video game cover image URL
 * @param releaseDate the video game release date
 * @param status      the current status in the user's library
 * @param addedAt     when the game was added to the library
 * @param updatedAt   when the status was last updated
 * @param notes       private notes attached to the entry (null when unset)
 * @param userRating  the library owner's rating on the half-star scale (0.5–5.0),
 *                    or null if they haven't rated the game
 */
public record UserGameResponseDto(
        UUID id,
        UUID videoGameId,
        String title,
        String coverUrl,
        LocalDate releaseDate,
        PlayStatus status,
        LocalDateTime addedAt,
        LocalDateTime updatedAt,
        String notes,
        Double userRating
) {}
