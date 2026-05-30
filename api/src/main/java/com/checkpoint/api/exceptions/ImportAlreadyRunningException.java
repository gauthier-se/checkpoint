package com.checkpoint.api.exceptions;

/**
 * Thrown when a bulk-import job is requested while another import is already
 * pending or running. Mapped to HTTP 409 Conflict.
 */
public class ImportAlreadyRunningException extends RuntimeException {

    public ImportAlreadyRunningException() {
        super("A bulk import is already running. Wait for it to finish before starting another.");
    }
}
