package com.checkpoint.api.services;

import com.checkpoint.api.entities.NewsSource;

/**
 * Orchestrates the import of news from external sources (Steam, RSS) into the
 * local {@code news} table. Each pass is independent and idempotent: items are
 * dedup'd against {@code (source, externalId)} so re-running is safe.
 */
public interface NewsImportService {

    /**
     * Imports the latest news for every catalog game with at least one user-game link.
     *
     * @return the number of news entries inserted by this run
     */
    int importSteamNews();

    /**
     * Fetches every configured RSS feed and imports the new entries.
     *
     * @return the number of news entries inserted by this run
     */
    int importRssFeeds();

    /**
     * Dispatches to {@link #importSteamNews()} or {@link #importRssFeeds()}.
     * {@link NewsSource#MANUAL} is rejected — manual news has no import pass.
     *
     * @param source the source to import from
     * @return the number of news entries inserted by this run
     * @throws IllegalArgumentException when {@code source} is {@code MANUAL}
     */
    int importFromSource(NewsSource source);
}
