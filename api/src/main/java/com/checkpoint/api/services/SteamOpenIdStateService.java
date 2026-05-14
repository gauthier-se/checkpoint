package com.checkpoint.api.services;

import java.util.Optional;

/**
 * Issues and verifies short-lived signed state tokens for the Steam OpenID 2.0 flow.
 *
 * <p>A state token is an HS256 JWT minted at {@code /api/auth/steam/openid/start} and
 * verified at {@code /api/auth/steam/openid/callback}. It binds the callback to the flow
 * we initiated by encoding:</p>
 *
 * <ul>
 *   <li>the requested {@code action} ({@code login} or {@code link})</li>
 *   <li>for {@code link}, the email of the user who initiated the flow</li>
 *   <li>a random nonce and an expiry</li>
 * </ul>
 *
 * <p>This is a defense-in-depth layer on top of Steam's {@code openid.response_nonce}.
 * It closes a social-engineering attack where an attacker who completed Steam auth as
 * themselves tricks an authenticated victim into following a {@code action=link} callback
 * URL, attaching the attacker's SteamID to the victim's account.</p>
 */
public interface SteamOpenIdStateService {

    /**
     * Mints a state token for the given action.
     *
     * @param action the action being initiated ({@code login} or {@code link})
     * @param email  the authenticated user's email for {@code action=link}; {@code null} for {@code login}
     * @return the signed JWT, safe to include verbatim as a query-string value
     */
    String issue(String action, String email);

    /**
     * Verifies a state token's signature, expiry, and {@code type} claim.
     *
     * <p>Returns {@link Optional#empty()} for any failure: null/blank token, malformed JWT,
     * bad signature, expired token, or wrong token type. Callers should treat all empty
     * results identically (reject the callback) — distinguishing the cause leaks information
     * to an attacker.</p>
     *
     * @param token the candidate JWT from the {@code state} query parameter
     * @return parsed claims on success, empty otherwise
     */
    Optional<Claims> verify(String token);

    /**
     * Claims extracted from a verified state token.
     *
     * @param action the action this state was issued for ({@code login} or {@code link})
     * @param email  the email of the user who initiated the flow; {@code null} for {@code login} states
     */
    record Claims(String action, String email) {}
}
