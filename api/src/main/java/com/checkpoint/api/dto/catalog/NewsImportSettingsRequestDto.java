package com.checkpoint.api.dto.catalog;

/**
 * Request DTO for updating the news-import settings. Every field is optional:
 * a null leaves the stored value untouched, which lets the panel send a single
 * toggle without restating the rest of the form.
 *
 * <p>{@code maxArticlesPerDay} therefore cannot be cleared by sending null.
 * {@code unlimited} is the explicit way to remove the ceiling.</p>
 *
 * @param steamEnabled      pause or resume the scheduled Steam pass
 * @param rssEnabled        pause or resume the scheduled RSS pass
 * @param maxArticlesPerDay new daily ceiling, ignored when {@code unlimited} is true
 * @param steamNewsPerGame  new per-game Steam item count
 * @param unlimited         when true, removes the daily ceiling entirely
 */
public record NewsImportSettingsRequestDto(
        Boolean steamEnabled,
        Boolean rssEnabled,
        Integer maxArticlesPerDay,
        Integer steamNewsPerGame,
        Boolean unlimited
) {}
