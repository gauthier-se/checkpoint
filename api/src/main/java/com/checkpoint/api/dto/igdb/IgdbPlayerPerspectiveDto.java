package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for IGDB Player Perspective response.
 * Represents player perspectives like First person, Third person, Bird view, etc.
 *
 * @see <a href="https://api-docs.igdb.com/#player-perspective">IGDB Player Perspective Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbPlayerPerspectiveDto(
        Long id,
        String name,
        String slug,
        String url
) {}
