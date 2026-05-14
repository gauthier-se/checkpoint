package com.checkpoint.api.client;

import java.util.List;
import java.util.Optional;

import com.checkpoint.api.dto.steam.SteamOwnedGameDto;
import com.checkpoint.api.dto.steam.SteamPlayerSummaryDto;

/**
 * Client for the Steam Web API.
 */
public interface SteamApiClient {

    /**
     * Fetches the public summary of a single Steam player.
     *
     * <p>Returns {@link Optional#empty()} when the SteamID is unknown to Steam
     * (Steam returns an empty {@code players} array), or when the API call fails
     * in a way that is recoverable for the caller.</p>
     *
     * @param steamId the 17-digit SteamID64
     * @return the player summary, or empty if the SteamID is not recognized
     */
    Optional<SteamPlayerSummaryDto> fetchPlayerSummary(String steamId);

    /**
     * Resolves a Steam vanity name (the slug after {@code /id/} on a profile URL)
     * to its 17-digit SteamID64.
     *
     * <p>Returns {@link Optional#empty()} when Steam reports no match
     * ({@code success == 42}).</p>
     *
     * @param vanity the vanity name (e.g. {@code "alice"})
     * @return the resolved SteamID64, or empty if the vanity is not recognized
     */
    Optional<String> resolveVanityUrl(String vanity);

    /**
     * Fetches every game owned by a Steam user via {@code IPlayerService/GetOwnedGames}
     * (with {@code include_appinfo=true} and {@code include_played_free_games=true}).
     *
     * <p>Returns an empty list when the library is private or the user owns nothing —
     * Steam represents both as an empty outer object. Visibility validation is the
     * caller's responsibility (see {@code SteamPlayerSummaryDto#communityVisibilityState}).</p>
     *
     * @param steamId the 17-digit SteamID64
     * @return the list of owned games (may be empty); never {@code null}
     */
    List<SteamOwnedGameDto> getOwnedGames(String steamId);
}
