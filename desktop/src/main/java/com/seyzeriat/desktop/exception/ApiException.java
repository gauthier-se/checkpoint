package com.seyzeriat.desktop.exception;

/**
 * Exception thrown when an error occurs during API communication.
 */
public class ApiException extends Exception {
    
    /**
     * Constructs a new ApiException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public ApiException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new ApiException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the underlying cause of the exception
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
