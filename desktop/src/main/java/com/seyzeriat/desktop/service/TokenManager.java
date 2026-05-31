package com.seyzeriat.desktop.service;

/**
 * Secure in-memory JWT token manager (singleton).
 *
 * <p>Stores the access token and refresh token in volatile fields so they are never persisted
 * to disk. Both tokens are lost when the application exits, forcing re-authentication on
 * the next launch.</p>
 */
public final class TokenManager {

    private static final TokenManager INSTANCE = new TokenManager();

    private volatile String token;
    private volatile String refreshToken;

    private TokenManager() {}

    /**
     * Returns the singleton instance of {@code TokenManager}.
     *
     * @return the token manager instance
     */
    public static TokenManager getInstance() {
        return INSTANCE;
    }

    /**
     * Stores the JWT access token.
     *
     * @param token the JWT access token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Returns the stored JWT access token, or {@code null} if not authenticated.
     */
    public String getToken() {
        return token;
    }

    /**
     * Stores the refresh token.
     *
     * @param refreshToken the opaque refresh token
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Returns the stored refresh token, or {@code null} if not available.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Clears both the access token and refresh token (logout / session expiry).
     */
    public void clear() {
        this.token = null;
        this.refreshToken = null;
    }

    /**
     * Returns {@code true} if an access token is currently stored.
     */
    public boolean isAuthenticated() {
        return token != null && !token.isBlank();
    }
}
