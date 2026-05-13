package com.checkpoint.api.dto.steam;

/**
 * Public representation of a linked Steam account.
 *
 * @param steamId         the SteamID64
 * @param steamDisplayName the user's Steam display name (persona name), or {@code null} if Steam is
 *                         unreachable or the profile is private
 * @param profileUrl      the canonical URL of the user's Steam profile, or {@code null}
 * @param avatarUrl       the URL of the user's medium avatar (64x64), or {@code null}
 */
public record SteamAccountDto(
        String steamId,
        String steamDisplayName,
        String profileUrl,
        String avatarUrl
) {}
