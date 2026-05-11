package com.checkpoint.api.services;

/**
 * Service interface for authenticated account-management operations
 * (operations that act on the currently authenticated user's account
 * rather than on a public profile).
 */
public interface AccountService {

    /**
     * Permanently deletes the account of the authenticated user along with
     * all of their personal data (GDPR Article 17 — right to erasure).
     *
     * <p>The deletion runs in a single transaction and follows a fixed order
     * so that foreign-key constraints are respected. After the call returns
     * successfully the user row no longer exists and any reference data
     * (notifications, refresh tokens, follow rows, etc.) has been removed.</p>
     *
     * @param email the email of the user to delete, taken from the
     *              authenticated principal
     */
    void deleteCurrentUser(String email);
}
