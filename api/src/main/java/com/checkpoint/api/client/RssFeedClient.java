package com.checkpoint.api.client;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client that fetches and parses an RSS or Atom feed using ROME.
 *
 * <p>HTML in {@code description} is stripped to plain text by the implementation
 * before being returned, so callers can safely persist it without re-escaping.</p>
 */
public interface RssFeedClient {

    /**
     * Fetches and parses the given feed.
     *
     * @param name a human-readable feed name (used for logging only)
     * @param url  the feed URL
     * @return the entries in publication order. Empty if the fetch or parse fails —
     *         the caller decides how to react. Never returns {@code null}.
     */
    List<RssItem> fetch(String name, String url);

    /**
     * A single feed entry after sanitization.
     *
     * @param guid        item GUID (or link as fallback)
     * @param title       item title
     * @param description plain-text description (all HTML tags stripped)
     * @param link        item link
     * @param publishedAt publication time, or {@code null} when the feed omits it
     * @param imageUrl    image URL recovered from an enclosure, a Media RSS
     *                    {@code <media:content>} / {@code <media:thumbnail>}, or
     *                    the first {@code <img>} embedded in the description.
     *                    {@code null} when no http(s) URL can be found.
     */
    record RssItem(
            String guid,
            String title,
            String description,
            String link,
            LocalDateTime publishedAt,
            String imageUrl
    ) {}
}
