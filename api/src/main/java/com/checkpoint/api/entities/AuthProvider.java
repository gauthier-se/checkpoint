package com.checkpoint.api.entities;

/**
 * Authentication provider for a {@link User} account.
 *
 * <ul>
 *   <li>{@link #LOCAL}: email/password account managed by CheckPoint.</li>
 *   <li>{@link #GOOGLE}: OAuth2 account authenticated through Google (OIDC).</li>
 *   <li>{@link #TWITCH}: OAuth2 account authenticated through Twitch.</li>
 * </ul>
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    TWITCH
}
