package com.checkpoint.api.exceptions;

/**
 * Thrown when a Steam library cannot be synced because the user's Steam profile or
 * game-details visibility is not public ({@code communityvisibilitystate != 3}).
 */
public class SteamLibraryPrivateException extends RuntimeException {

    public SteamLibraryPrivateException(String message) {
        super(message);
    }
}
