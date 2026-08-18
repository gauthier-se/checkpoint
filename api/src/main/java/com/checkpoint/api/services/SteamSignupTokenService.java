package com.checkpoint.api.services;

import java.util.Optional;

/**
 * Issues and verifies short-lived signed tokens that carry a Steam identity from the
 * {@code /api/v1/auth/steam/openid/callback} redirect to the {@code /register} signup form.
 *
 * <p>A token is an HS256 JWT minted when a user authenticates via Steam OpenID but has
 * no CheckPoint account linked yet. The token is delivered to the browser as the
 * {@code ?steam_token=} query parameter on the redirect, then exchanged for a prefill
 * payload at {@code /api/v1/auth/steam/signup-prefill} and finally presented at
 * {@code /api/v1/auth/register/steam} to create the account.</p>
 *
 * <p>The token must carry the verified SteamID plus any Steam profile fields that were
 * available at issuance time. Email is deliberately <em>not</em> part of the token:
 * the user supplies it on the signup form, which prevents an attacker from squatting
 * an arbitrary email using a Steam identity they control.</p>
 */
public interface SteamSignupTokenService {

    /**
     * Mints a Steam signup token carrying the verified Steam identity.
     *
     * @param steamId          the verified 64-bit SteamID as a string (required)
     * @param steamDisplayName the persona name from Steam, or {@code null} if unavailable
     * @param steamAvatarUrl   the medium avatar URL from Steam, or {@code null} if unavailable
     * @param steamProfileUrl  the public profile URL from Steam, or {@code null} if unavailable
     * @return the signed JWT, safe to include verbatim as a query-string value
     */
    String issue(String steamId, String steamDisplayName, String steamAvatarUrl, String steamProfileUrl);

    /**
     * Verifies a Steam signup token's signature, expiry, and {@code type} claim.
     *
     * <p>Returns {@link Optional#empty()} for any failure: null/blank token, malformed JWT,
     * bad signature, expired token, or wrong token type. Callers should treat all empty
     * results identically: distinguishing the cause leaks information to an attacker.</p>
     *
     * @param token the candidate JWT from the {@code steam_token} query parameter
     * @return parsed claims on success, empty otherwise
     */
    Optional<Claims> verify(String token);

    /**
     * Claims extracted from a verified Steam signup token.
     *
     * @param steamId           the verified SteamID (always non-null on a verified token)
     * @param steamDisplayName  the persona name; may be {@code null}
     * @param steamAvatarUrl    the medium avatar URL; may be {@code null}
     * @param steamProfileUrl   the public profile URL; may be {@code null}
     */
    record Claims(String steamId, String steamDisplayName, String steamAvatarUrl, String steamProfileUrl) {
    }
}
