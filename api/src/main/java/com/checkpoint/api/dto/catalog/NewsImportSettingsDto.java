package com.checkpoint.api.dto.catalog;

import java.time.LocalDateTime;

/**
 * Response DTO for the admin news-import settings, with the live counters the
 * panel needs to explain what the ceiling is doing right now.
 *
 * @param steamEnabled      whether the scheduled Steam pass runs
 * @param rssEnabled        whether the scheduled RSS pass runs
 * @param maxArticlesPerDay daily ceiling across both importers, or null for no ceiling
 * @param steamNewsPerGame  how many Steam items are requested per game
 * @param importedToday     articles the importers inserted since midnight
 * @param remainingToday    what is left of the ceiling today, or null when uncapped
 * @param updatedAt         when the settings were last saved
 * @param updatedBy         username of the admin who last saved them, null if never edited
 */
public record NewsImportSettingsDto(
        boolean steamEnabled,
        boolean rssEnabled,
        Integer maxArticlesPerDay,
        int steamNewsPerGame,
        long importedToday,
        Integer remainingToday,
        LocalDateTime updatedAt,
        String updatedBy
) {}
