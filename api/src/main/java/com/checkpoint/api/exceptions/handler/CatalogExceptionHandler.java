package com.checkpoint.api.exceptions.handler;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.checkpoint.api.dto.error.ErrorResponse;
import com.checkpoint.api.dto.error.GameReferencedResponse;
import com.checkpoint.api.exceptions.ExternalApiUnavailableException;
import com.checkpoint.api.exceptions.ExternalGameNotFoundException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameReferencedException;
import com.checkpoint.api.exceptions.IgdbApiException;
import com.checkpoint.api.exceptions.ImportAlreadyRunningException;
import com.checkpoint.api.exceptions.NewsNotFoundException;

/**
 * Handles exceptions raised by the game catalog, external game providers
 * (IGDB), admin game management, and news endpoints.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class CatalogExceptionHandler extends AbstractExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CatalogExceptionHandler.class);

    /** Handles {@link GameNotFoundException}. */
    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameNotFound(GameNotFoundException ex) {
        log.warn("Game not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link ExternalGameNotFoundException} when a game is not found in IGDB. */
    @ExceptionHandler(ExternalGameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleExternalGameNotFound(ExternalGameNotFoundException ex) {
        log.warn("External game not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link ExternalApiUnavailableException} when IGDB is down or rate limited. */
    @ExceptionHandler(ExternalApiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleExternalApiUnavailable(ExternalApiUnavailableException ex) {
        log.error("External API unavailable: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /** Handles {@link IgdbApiException} as a safety net when not re-wrapped by a service. */
    @ExceptionHandler(IgdbApiException.class)
    public ResponseEntity<ErrorResponse> handleIgdbApiException(IgdbApiException ex) {
        log.error("IGDB API error: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /** Handles {@link ImportAlreadyRunningException} when a bulk import is already in progress. */
    @ExceptionHandler(ImportAlreadyRunningException.class)
    public ResponseEntity<ErrorResponse> handleImportAlreadyRunning(ImportAlreadyRunningException ex) {
        log.warn("Import already running: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link NewsNotFoundException}. */
    @ExceptionHandler(NewsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNewsNotFound(NewsNotFoundException ex) {
        log.warn("News not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles {@link GameReferencedException} when an admin attempts to delete a
     * game still referenced by user-owned data or DLC entries. Returns 409 with
     * the per-category blocking counts.
     */
    @ExceptionHandler(GameReferencedException.class)
    public ResponseEntity<GameReferencedResponse> handleGameReferenced(GameReferencedException ex) {
        log.warn("Game referenced — refusing delete: {}", ex.getMessage());

        GameReferencedResponse body = new GameReferencedResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Game cannot be deleted because it is referenced by existing data",
                LocalDateTime.now(),
                ex.getBlockingReferences()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
