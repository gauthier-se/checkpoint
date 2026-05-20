package com.checkpoint.api.entities;

/**
 * Identifies the origin of a {@link News} entry.
 *
 * <p>{@link #MANUAL} entries are authored by an admin via the desktop panel and carry
 * a non-null {@code author}. {@link #STEAM} and {@link #RSS} entries are produced by
 * the news import task and have a null {@code author}; their attribution lives in
 * {@code feedName} and {@code externalUrl}.</p>
 */
public enum NewsSource {
    MANUAL,
    STEAM,
    RSS
}
