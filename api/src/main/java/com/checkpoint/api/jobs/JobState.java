package com.checkpoint.api.jobs;

/**
 * Lifecycle state of an asynchronous bulk-import job.
 */
public enum JobState {
    /** Created but the worker has not started yet. */
    PENDING,
    /** The worker is actively importing games. */
    RUNNING,
    /** Finished successfully. */
    COMPLETED,
    /** Aborted by a fatal error (see {@code errorMessage}). */
    FAILED
}
