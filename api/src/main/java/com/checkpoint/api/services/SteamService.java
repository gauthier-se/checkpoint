package com.checkpoint.api.services;

import java.util.Optional;

import com.checkpoint.api.dto.steam.SteamAccountDto;
import com.checkpoint.api.entities.User;

/**
 * Service for linking, unlinking, and looking up Steam accounts on user profiles.
 */
public interface SteamService {

    /**
     * Links a Steam account to the user identified by {@code email}, after first verifying that
     * the given SteamID64 is recognized by the Steam Web API.
     *
     * <p>Used by {@code POST /api/me/steam/link} when the user types their SteamID manually.</p>
     *
     * @param email   the authenticated user's email
     * @param steamId the 17-digit SteamID64 supplied by the user
     * @return a DTO describing the freshly linked account
     * @throws com.checkpoint.api.exceptions.InvalidSteamIdException if Steam does not recognize the ID
     */
    SteamAccountDto linkSteamAccount(String email, String steamId);

    /**
     * Links a Steam account to the user identified by {@code email} using a SteamID64 that has
     * already been authenticated via the OpenID 2.0 flow. No additional validation is performed
     * because OpenID has already established ownership.
     *
     * @param email   the authenticated user's email
     * @param steamId the SteamID64 extracted from a verified OpenID callback
     * @return a DTO describing the freshly linked account
     */
    SteamAccountDto linkVerifiedSteamAccount(String email, String steamId);

    /**
     * Clears any linked Steam account for the user identified by {@code email}.
     *
     * @param email the authenticated user's email
     */
    void unlinkSteamAccount(String email);

    /**
     * Looks up the user who has the given SteamID64 linked, for use in the OpenID login flow.
     *
     * @param steamId the SteamID64 returned by a verified OpenID callback
     * @return the user, or empty if no account is linked to this SteamID
     */
    Optional<User> findUserBySteamId(String steamId);

    /**
     * Best-effort lookup of the display name and avatar for a stored SteamID, used to enrich
     * the {@code /api/auth/me} response. Returns empty if Steam is unreachable or the profile
     * has been removed.
     *
     * @param steamId the SteamID64
     * @return the account info, or empty if Steam cannot be queried
     */
    Optional<SteamAccountDto> getLinkedAccount(String steamId);
}
