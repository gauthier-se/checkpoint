package com.checkpoint.api.exceptions;

/**
 * Thrown when an error occurs while communicating with the Steam Web API.
 */
public class SteamApiException extends RuntimeException {

    public SteamApiException(String message) {
        super(message);
    }

    public SteamApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
