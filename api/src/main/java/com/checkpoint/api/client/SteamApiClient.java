package com.checkpoint.api.client;

import java.util.Optional;

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
}
