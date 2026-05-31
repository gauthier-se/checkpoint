package com.seyzeriat.desktop.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends Exception {
    
    /**
     * Constructs a new AuthenticationException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
