package com.checkpoint.api.exceptions;

/**
 * Exception thrown when a favorites update request is structurally invalid
 * beyond what bean validation can express (e.g., duplicate gameIds).
 */
public class InvalidFavoritesException extends RuntimeException {

    /**
     * Constructs a new InvalidFavoritesException.
     *
     * @param message the human-readable reason the request was rejected
     */
    public InvalidFavoritesException(String message) {
        super(message);
    }
}
