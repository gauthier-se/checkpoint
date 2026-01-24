package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for IGDB Game Mode response.
 * Represents game modes like Single player, Multiplayer, Co-operative, etc.
 *
 * @see <a href="https://api-docs.igdb.com/#game-mode">IGDB Game Mode Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbGameModeDto(
        Long id,
        String name,
        String slug,
        String url
) {}
