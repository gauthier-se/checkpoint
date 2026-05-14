package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Row returned by IGDB's {@code /external_games} endpoint, used to bridge external store
 * identifiers (Steam appIds, GOG IDs, ...) to IGDB game IDs.
 *
 * <p>For Steam lookups (category 1), {@code uid} contains the Steam appId as a string.</p>
 *
 * @param id   the external-game row's own IGDB ID
 * @param uid  the external store identifier (Steam appId as a string for category 1)
 * @param game the IGDB game ID this external entry maps to (may be null for orphan rows)
 */
public record IgdbExternalGameDto(
        @JsonProperty("id") Long id,
        @JsonProperty("uid") String uid,
        @JsonProperty("game") Long game
) {}
