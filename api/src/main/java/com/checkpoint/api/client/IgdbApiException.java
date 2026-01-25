package com.checkpoint.api.client;

/**
 * Exception thrown when an error occurs while communicating with the IGDB API.
 */
public class IgdbApiException extends RuntimeException {

    public IgdbApiException(String message) {
        super(message);
    }

    public IgdbApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
