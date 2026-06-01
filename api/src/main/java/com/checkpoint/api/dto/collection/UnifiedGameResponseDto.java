package com.checkpoint.api.dto.collection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.enums.Priority;

/**
 * Unified response DTO for the "All games" view, aggregating library, wishlist,
 * backlog, and liked entries into a single deduplicated, paginated feed. A game
 * that appears in multiple collections is represented once with all its
 * collection types listed.
 *
 * @param videoGameId     the video game ID (unique key in the feed)
 * @param title           the video game title
 * @param coverUrl        the video game cover image URL
 * @param releaseDate     the video game release date
 * @param collectionTypes all collections this game belongs to: any subset of
 *                        {@code LIBRARY}, {@code WISHLIST}, {@code BACKLOG},
 *                        {@code LIKED}
 * @param addedAt         the most recent interaction date across all collections
 * @param libraryStatus   play status — non-null when {@code LIBRARY} is present
 * @param userRating      the owner's rating (0.5–5.0) — non-null when rated
 * @param priority        the user-assigned priority — non-null when
 *                        {@code WISHLIST} or {@code BACKLOG} is present
 */
public record UnifiedGameResponseDto(
        UUID videoGameId,
        String title,
        String coverUrl,
        LocalDate releaseDate,
        List<String> collectionTypes,
        LocalDateTime addedAt,
        PlayStatus libraryStatus,
        Double userRating,
        Priority priority
) {}
