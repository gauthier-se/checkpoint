package com.checkpoint.api.exceptions;

/**
 * Thrown when a SteamID64 is malformed or not recognized by the Steam Web API.
 */
public class InvalidSteamIdException extends RuntimeException {

    public InvalidSteamIdException(String message) {
        super(message);
    }
}
