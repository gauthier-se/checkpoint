package com.checkpoint.api.services;

import java.util.List;
import java.util.UUID;

import com.checkpoint.api.dto.profile.FavoriteDto;

/**
 * Service for managing a user's favorite games (top 5, ordered).
 */
public interface FavoriteService {

    /**
     * Retrieves the favorite games for a user, ordered by {@code displayOrder} ascending.
     *
     * @param userId the user's ID
     * @return the user's favorites in display order (0 to 5 entries)
     */
    List<FavoriteDto> getFavorites(UUID userId);

    /**
     * Replaces the authenticated user's favorites with the provided ordered list of game IDs.
     * The list's index becomes the resulting {@code displayOrder} (0-based).
     * The operation runs in a single transaction.
     *
     * @param email           the authenticated user's email
     * @param orderedGameIds  the new ordered list of game IDs (0 to 5 unique entries)
     * @return the resulting favorites in display order
     */
    List<FavoriteDto> replaceFavorites(String email, List<UUID> orderedGameIds);
}
