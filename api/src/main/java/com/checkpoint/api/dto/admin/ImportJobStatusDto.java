package com.checkpoint.api.dto.admin;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot of an asynchronous bulk-import job, returned by the start endpoint
 * (202 Accepted) and polled via the job-status endpoint.
 *
 * @param jobId          the job identifier (UUID string)
 * @param type           the import type ({@code TOP_RATED} or {@code RECENT})
 * @param state          the job state ({@code PENDING}, {@code RUNNING}, {@code COMPLETED}, {@code FAILED})
 * @param requestedLimit the number of games requested
 * @param minRatingCount the minimum IGDB rating count (0 for recent imports)
 * @param totalFetched   the number of games fetched from IGDB (0 until known)
 * @param processed      the number of games processed so far
 * @param imported       the number of newly persisted games
 * @param skipped        the number of games skipped because they already existed
 * @param failed         the number of games whose import threw an exception
 * @param errors         titles (or IGDB IDs) of failed games, capped for size
 * @param errorMessage   a fatal error message when the whole job failed, else {@code null}
 * @param startedAt      when the job was created
 * @param finishedAt     when the job reached a terminal state, or {@code null} while running
 */
public record ImportJobStatusDto(
        String jobId,
        String type,
        String state,
        int requestedLimit,
        int minRatingCount,
        int totalFetched,
        int processed,
        int imported,
        int skipped,
        int failed,
        List<String> errors,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {}
