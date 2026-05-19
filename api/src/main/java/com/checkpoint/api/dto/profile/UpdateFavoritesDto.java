package com.checkpoint.api.dto.profile;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for replacing the authenticated user's favorite games.
 * The list order defines the resulting {@code displayOrder} (0-based).
 *
 * @param gameIds the ordered list of game IDs (max 5)
 */
public record UpdateFavoritesDto(
        @NotNull(message = "gameIds is required")
        @Size(max = 5, message = "A user can have at most 5 favorite games")
        List<UUID> gameIds
) {}
