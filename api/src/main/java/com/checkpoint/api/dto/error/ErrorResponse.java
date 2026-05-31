package com.checkpoint.api.dto.error;

import java.time.LocalDateTime;

/**
 * Standard error response returned by the API exception handlers.
 *
 * @param status    the HTTP status code
 * @param error     the HTTP reason phrase (e.g. "Not Found")
 * @param message   a human-readable explanation of the error
 * @param timestamp when the error was produced
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {}
