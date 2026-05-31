package com.seyzeriat.desktop.exception;

/**
 * Exception thrown when a user attempts to access a resource without sufficient authorization.
 */
public class UnauthorizedException extends Exception {
    
    /**
     * Constructs a new UnauthorizedException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
