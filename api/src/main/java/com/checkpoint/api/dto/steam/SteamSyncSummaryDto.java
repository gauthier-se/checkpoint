package com.checkpoint.api.dto.steam;

/**
 * Outcome counts of a Steam library sync.
 *
 * @param total     total Steam games owned by the user
 * @param imported  games newly added to the user's CheckPoint library (status {@code BACKLOG})
 * @param skipped   games that matched IGDB but were already in the user's library
 * @param unmatched Steam games that have no matching IGDB entry
 */
public record SteamSyncSummaryDto(
        int total,
        int imported,
        int skipped,
        int unmatched
) {}
