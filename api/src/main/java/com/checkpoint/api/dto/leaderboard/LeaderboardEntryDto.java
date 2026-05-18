package com.checkpoint.api.dto.leaderboard;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing a single row in the public leaderboard.
 *
 * @param rank    the 1-based rank in the leaderboard
 * @param id      the user's UUID
 * @param pseudo  the user's display name
 * @param picture the user's profile picture URL (nullable)
 * @param level   the user's level
 * @param xpPoint the user's XP
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeaderboardEntryDto(
        int rank,
        UUID id,
        String pseudo,
        String picture,
        Integer level,
        Integer xpPoint
) {}
