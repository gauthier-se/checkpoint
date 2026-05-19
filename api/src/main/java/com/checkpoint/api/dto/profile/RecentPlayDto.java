package com.checkpoint.api.dto.profile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a compact recent play entry on a user's profile.
 *
 * @param id          the play log ID (used for the /plays/$id detail link)
 * @param videoGameId the video game ID
 * @param title       the game title
 * @param coverUrl    the game cover image URL (may be null)
 * @param score       the play score, 1-10 half-star steps (display = score / 2); may be null
 * @param hasReview   true if the play has an associated review
 * @param isReplay    true if the play is flagged as a replay
 * @param isLiked     true if the profile owner has liked this video game
 * @param createdAt   when the play log was created
 */
public record RecentPlayDto(
        UUID id,
        UUID videoGameId,
        String title,
        String coverUrl,
        Integer score,
        Boolean hasReview,
        Boolean isReplay,
        Boolean isLiked,
        LocalDateTime createdAt
) {}
