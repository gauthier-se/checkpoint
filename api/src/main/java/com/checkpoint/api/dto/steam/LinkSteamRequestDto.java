package com.checkpoint.api.dto.steam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code POST /api/me/steam/link}.
 *
 * @param steamId a 17-digit SteamID64 (numeric string)
 */
public record LinkSteamRequestDto(
        @NotBlank
        @Pattern(regexp = "\\d{17}", message = "steamId must be a 17-digit numeric SteamID64")
        String steamId
) {}
