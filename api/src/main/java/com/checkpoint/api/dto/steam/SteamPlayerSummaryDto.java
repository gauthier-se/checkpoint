package com.checkpoint.api.dto.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Single player summary returned by Steam's {@code ISteamUser/GetPlayerSummaries} endpoint.
 * Only the fields used by CheckPoint are mapped.
 *
 * @param steamId       the 64-bit SteamID as a string
 * @param personaName   the user's display name on Steam
 * @param profileUrl    the canonical URL of the user's Steam profile
 * @param avatar        the URL of the user's small avatar (32x32)
 * @param avatarMedium  the URL of the user's medium avatar (64x64)
 * @param avatarFull    the URL of the user's full avatar (184x184)
 */
public record SteamPlayerSummaryDto(
        @JsonProperty("steamid") String steamId,
        @JsonProperty("personaname") String personaName,
        @JsonProperty("profileurl") String profileUrl,
        @JsonProperty("avatar") String avatar,
        @JsonProperty("avatarmedium") String avatarMedium,
        @JsonProperty("avatarfull") String avatarFull
) {}
