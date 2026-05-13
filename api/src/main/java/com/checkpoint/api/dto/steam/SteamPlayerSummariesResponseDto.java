package com.checkpoint.api.dto.steam;

import java.util.List;

/**
 * Wrapper for the Steam {@code GetPlayerSummaries} response envelope:
 * {@code { "response": { "players": [...] } }}.
 *
 * @param response the inner response payload
 */
public record SteamPlayerSummariesResponseDto(SteamResponse response) {

    /**
     * Inner payload of {@link SteamPlayerSummariesResponseDto}.
     *
     * @param players the list of player summaries (empty if the SteamID is unknown)
     */
    public record SteamResponse(List<SteamPlayerSummaryDto> players) {}
}
