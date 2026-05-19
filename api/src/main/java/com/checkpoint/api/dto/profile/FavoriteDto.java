package com.checkpoint.api.dto.profile;

import java.util.UUID;

/**
 * DTO representing one of a user's favorite games on their profile.
 *
 * @param gameId      the video game ID
 * @param title       the game title
 * @param coverUrl    the game cover image URL (may be null)
 * @param displayOrder the slot index (0-based, 0..4)
 */
public record FavoriteDto(
        UUID gameId,
        String title,
        String coverUrl,
        Integer displayOrder
) {}
