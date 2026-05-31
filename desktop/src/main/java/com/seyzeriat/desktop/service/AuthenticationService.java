package com.seyzeriat.desktop.service;

import com.seyzeriat.desktop.exception.AuthenticationException;

/**
 * Service interface for managing user authentication.
 * Provides operations to login, logout, and refresh access tokens.
 */
public interface AuthenticationService {

    /**
     * Authenticates a user with their email and password.
     *
     * @param email the user's email address
     * @param password the user's password
     * @throws AuthenticationException if the authentication fails
     */
    void login(String email, String password) throws AuthenticationException;

    /**
     * Refreshes the authentication tokens using the current refresh token.
     *
     * @throws AuthenticationException if the token refresh fails
     */
    void refreshTokens() throws AuthenticationException;

    /**
     * Logs out the current user by clearing the authentication tokens.
     */
    void logout();
}
