package com.checkpoint.api.exceptions;

/**
 * Thrown when a Steam OpenID 2.0 callback fails verification or returns a malformed payload.
 */
public class SteamOpenIdException extends RuntimeException {

    public SteamOpenIdException(String message) {
        super(message);
    }

    public SteamOpenIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
