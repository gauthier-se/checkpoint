package com.checkpoint.api.exceptions;

/**
 * Thrown when an operation that requires a linked Steam account (e.g. library sync)
 * is invoked by a user who has not linked one yet.
 */
public class SteamAccountNotLinkedException extends RuntimeException {

    public SteamAccountNotLinkedException(String message) {
        super(message);
    }
}
