package com.checkpoint.api.jobs;

/**
 * The kind of bulk import a job performs.
 */
public enum ImportType {
    /** Most well-known games, ordered by IGDB rating count. */
    TOP_RATED,
    /** Recently released games. */
    RECENT
}
