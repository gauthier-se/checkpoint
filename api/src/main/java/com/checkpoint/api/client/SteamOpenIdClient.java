package com.checkpoint.api.client;

import java.util.Map;

/**
 * Client for the Steam OpenID 2.0 authentication flow.
 *
 * <p>Steam does not support OAuth2; instead it implements OpenID 2.0, which works by:
 * (1) redirecting the user to Steam with a {@code checkid_setup} request,
 * (2) Steam redirecting them back to our callback with signed {@code openid.*} parameters,
 * (3) us POSTing those parameters back to Steam with {@code openid.mode=check_authentication}
 *     so Steam can verify their authenticity.</p>
 */
public interface SteamOpenIdClient {

    /**
     * Builds the full OpenID 2.0 redirect URL that the user must be sent to.
     *
     * @param returnUrl the absolute URL Steam should redirect back to after authentication
     * @param realm     the OpenID realm (typically the scheme + host portion of {@code returnUrl})
     * @return the absolute Steam authentication URL
     */
    String buildAuthenticationUrl(String returnUrl, String realm);

    /**
     * Verifies the {@code openid.*} parameters returned by Steam in the callback by re-posting
     * them with {@code openid.mode=check_authentication}, then extracts the SteamID64
     * from {@code openid.claimed_id}.
     *
     * @param params the {@code openid.*} parameters received on the callback request
     * @return the verified SteamID64
     * @throws com.checkpoint.api.exceptions.SteamOpenIdException if the payload is malformed
     *         or Steam rejects the verification
     */
    String verifyAndExtractSteamId(Map<String, String> params);
}
