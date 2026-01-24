package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for IGDB Theme response.
 * Represents game themes like Action, Fantasy, Science fiction, etc.
 *
 * @see <a href="https://api-docs.igdb.com/#theme">IGDB Theme Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbThemeDto(
        Long id,
        String name,
        String slug,
        String url
) {}
