package com.checkpoint.api.services;

import com.checkpoint.api.dto.catalog.NewsImportSettingsDto;
import com.checkpoint.api.dto.catalog.NewsImportSettingsRequestDto;
import com.checkpoint.api.entities.NewsSource;

/**
 * Reads and writes the admin-editable knobs of the news importers.
 *
 * <p>Two audiences, hence the split in this interface: the admin panel gets the
 * whole picture as a DTO, while the import path only asks the narrow questions it
 * needs ({@link #isScheduledPassEnabled}, {@link #remainingDailyBudget},
 * {@link #steamNewsPerGame}) and never sees the entity.</p>
 */
public interface NewsImportSettingsService {

    /**
     * Returns the current settings along with today's counters, creating the row
     * with its defaults if no admin has saved the form yet.
     *
     * @return the settings and the state of today's ceiling
     */
    NewsImportSettingsDto get();

    /**
     * Applies a partial update. Null fields are left unchanged.
     *
     * @param adminUsername the admin saving the form, recorded for auditing
     * @param request       the partial update payload
     * @return the settings as they now stand, with refreshed counters
     * @throws IllegalArgumentException when a numeric field is out of bounds
     */
    NewsImportSettingsDto update(String adminUsername, NewsImportSettingsRequestDto request);

    /**
     * Returns whether the scheduled pass for the given source should run. Only the
     * scheduled passes consult this: an admin clicking Import in the panel is an
     * explicit act that a paused source does not veto.
     *
     * @param source the source whose scheduled pass is about to fire
     * @return true when the pass may run
     */
    boolean isScheduledPassEnabled(NewsSource source);

    /**
     * Returns how many articles the importers may still insert today, across all
     * external sources. Returns {@link Integer#MAX_VALUE} when no ceiling is set.
     *
     * @return the remaining budget, never negative
     */
    int remainingDailyBudget();

    /**
     * Returns how many news items to request per game from Steam.
     *
     * @return the configured per-game item count
     */
    int steamNewsPerGame();
}
