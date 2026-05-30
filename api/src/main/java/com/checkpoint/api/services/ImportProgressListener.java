package com.checkpoint.api.services;

/**
 * Callback invoked by {@link GameImportService#bulkImport(java.util.List, ImportProgressListener)}
 * after each game is processed, so a long-running asynchronous import can expose
 * live progress. Implementations must be thread-safe if read from another thread
 * (the async job status uses atomic counters).
 */
public interface ImportProgressListener {

    /** Called exactly once per game, regardless of outcome (imported/skipped/failed). */
    void processed();

    /** Called when a new game was persisted. */
    void imported();

    /** Called when a game was skipped because it already exists. */
    void skipped();

    /**
     * Called when a game failed to import.
     *
     * @param label the game title (or {@code IGDB#<id>}) for diagnostics
     */
    void failed(String label);

    /** A listener that ignores every callback — used by the synchronous import path. */
    ImportProgressListener NOOP = new ImportProgressListener() {
        @Override
        public void processed() {
        }

        @Override
        public void imported() {
        }

        @Override
        public void skipped() {
        }

        @Override
        public void failed(String label) {
        }
    };
}
