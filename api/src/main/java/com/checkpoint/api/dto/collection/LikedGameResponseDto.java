package com.checkpoint.api.dto.collection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a game the user has liked.
 *
 * @param id          the like association ID
 * @param videoGameId the video game ID
 * @param title       the video game title
 * @param coverUrl    the video game cover image URL
 * @param releaseDate the video game release date
 * @param likedAt     when the game was liked
 */
public record LikedGameResponseDto(
        UUID id,
        UUID videoGameId,
        String title,
        String coverUrl,
        LocalDate releaseDate,
        LocalDateTime likedAt
) {}
