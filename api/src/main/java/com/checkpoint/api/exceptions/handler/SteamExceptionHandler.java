package com.checkpoint.api.exceptions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.checkpoint.api.dto.error.ErrorResponse;
import com.checkpoint.api.exceptions.InvalidSteamIdException;
import com.checkpoint.api.exceptions.SteamAccountNotLinkedException;
import com.checkpoint.api.exceptions.SteamApiException;
import com.checkpoint.api.exceptions.SteamLibraryPrivateException;
import com.checkpoint.api.exceptions.SteamOpenIdException;

/**
 * Handles exceptions raised by Steam integration: account linking, library
 * synchronization, and OpenID sign-in.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SteamExceptionHandler extends AbstractExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SteamExceptionHandler.class);

    /** Handles {@link InvalidSteamIdException} when a SteamID is malformed or unknown to Steam. */
    @ExceptionHandler(InvalidSteamIdException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSteamId(InvalidSteamIdException ex) {
        log.warn("Invalid Steam ID: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Handles {@link SteamApiException} when the Steam Web API call fails. */
    @ExceptionHandler(SteamApiException.class)
    public ResponseEntity<ErrorResponse> handleSteamApiException(SteamApiException ex) {
        log.error("Steam API error: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /** Handles {@link SteamAccountNotLinkedException} when an action requires a linked Steam account. */
    @ExceptionHandler(SteamAccountNotLinkedException.class)
    public ResponseEntity<ErrorResponse> handleSteamAccountNotLinked(SteamAccountNotLinkedException ex) {
        log.warn("Steam account not linked: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Handles {@link SteamLibraryPrivateException} when the user's Steam library visibility blocks reads. */
    @ExceptionHandler(SteamLibraryPrivateException.class)
    public ResponseEntity<ErrorResponse> handleSteamLibraryPrivate(SteamLibraryPrivateException ex) {
        log.warn("Steam library private: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Handles {@link SteamOpenIdException} when Steam OpenID verification fails. */
    @ExceptionHandler(SteamOpenIdException.class)
    public ResponseEntity<ErrorResponse> handleSteamOpenId(SteamOpenIdException ex) {
        log.warn("Steam OpenID error: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
