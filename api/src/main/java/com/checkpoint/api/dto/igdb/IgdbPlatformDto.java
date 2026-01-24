package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for IGDB Platform response.
 *
 * @see <a href="https://api-docs.igdb.com/#platform">IGDB Platform Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbPlatformDto(
        Long id,
        String name,
        String slug,
        String abbreviation,

        @JsonProperty("alternative_name")
        String alternativeName,

        Integer generation,

        @JsonProperty("platform_logo")
        IgdbPlatformLogoDto platformLogo,

        String summary,
        String url
) {}
