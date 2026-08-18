package com.checkpoint.api.dto.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper for the Steam {@code ResolveVanityURL} response envelope:
 * {@code { "response": { "steamid": "...", "success": 1 } }}.
 *
 * <p>Success codes returned by Steam:
 * <ul>
 *   <li>{@code 1}: vanity resolved, {@code steamid} is populated</li>
 *   <li>{@code 42}: no match for the supplied vanity</li>
 * </ul>
 *
 * @param response the inner response payload
 */
public record SteamResolveVanityResponseDto(SteamResolveVanityResponse response) {

    /**
     * Inner payload of {@link SteamResolveVanityResponseDto}.
     *
     * @param steamId the resolved 17-digit SteamID64 (only present when {@code success == 1})
     * @param success Steam's status code ({@code 1} on success, {@code 42} on no match)
     * @param message human-readable status (typically {@code "No match"} when {@code success == 42})
     */
    public record SteamResolveVanityResponse(
            @JsonProperty("steamid") String steamId,
            Integer success,
            String message
    ) {}
}
