package com.checkpoint.api.dto.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single owned-game entry returned by Steam's {@code IPlayerService/GetOwnedGames} endpoint.
 * Only the fields used by CheckPoint are mapped — {@code appId} is the load-bearing one;
 * {@code name}, {@code playtimeForever}, and {@code imgIconUrl} are kept for logging and
 * possible future UI use.
 *
 * @param appId           the Steam application ID
 * @param name            the game's display name as shown on Steam
 * @param playtimeForever total playtime in minutes (all-time)
 * @param imgIconUrl      partial path to the game's icon (Steam serves the full URL via CDN)
 */
public record SteamOwnedGameDto(
        @JsonProperty("appid") Long appId,
        @JsonProperty("name") String name,
        @JsonProperty("playtime_forever") Long playtimeForever,
        @JsonProperty("img_icon_url") String imgIconUrl
) {}
