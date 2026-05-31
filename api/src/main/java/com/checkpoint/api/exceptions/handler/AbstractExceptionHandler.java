package com.checkpoint.api.exceptions.handler;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.checkpoint.api.dto.error.ErrorResponse;

/**
 * Base class for the domain-scoped {@code @RestControllerAdvice} handlers.
 *
 * <p>Centralizes the {@link ErrorResponse} construction so each handler only has
 * to map an exception to an HTTP status and a message. The {@code error} field
 * is derived from {@link HttpStatus#getReasonPhrase()} to keep responses
 * consistent across the API.</p>
 */
public abstract class AbstractExceptionHandler {

    /**
     * Builds a standard error response for the given status and message.
     *
     * @param status  the HTTP status to return
     * @param message the human-readable error message
     * @return a {@link ResponseEntity} wrapping an {@link ErrorResponse}
     */
    protected ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }
}
