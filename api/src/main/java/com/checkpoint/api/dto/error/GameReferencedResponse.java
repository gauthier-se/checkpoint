package com.checkpoint.api.dto.error;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Error response used when a game cannot be deleted because it is still
 * referenced by existing data, including the per-category counts of the
 * references blocking the deletion.
 *
 * @param status             the HTTP status code
 * @param error              the HTTP reason phrase (e.g. "Conflict")
 * @param message            a human-readable explanation of the error
 * @param timestamp          when the error was produced
 * @param blockingReferences per-category counts of references blocking deletion
 */
public record GameReferencedResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        Map<String, Long> blockingReferences
) {}
