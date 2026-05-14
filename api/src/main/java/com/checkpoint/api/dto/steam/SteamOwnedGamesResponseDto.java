package com.checkpoint.api.dto.steam;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper for Steam's {@code IPlayerService/GetOwnedGames} response.
 *
 * <p>Steam returns the data inside an outer {@code response} object. When the library is
 * private the outer object is empty ({@code {"response": {}}}) — both nested fields are
 * therefore nullable.</p>
 */
public record SteamOwnedGamesResponseDto(
        @JsonProperty("response") SteamOwnedGamesResponse response
) {
    public record SteamOwnedGamesResponse(
            @JsonProperty("game_count") Integer gameCount,
            @JsonProperty("games") List<SteamOwnedGameDto> games
    ) {}
}
