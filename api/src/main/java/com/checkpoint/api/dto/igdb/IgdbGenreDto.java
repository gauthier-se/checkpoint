package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for IGDB Genre response.
 *
 * @see <a href="https://api-docs.igdb.com/#genre">IGDB Genre Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbGenreDto(
        Long id,
        String name,
        String slug,
        String url
) {}
