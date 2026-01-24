package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for IGDB Company response.
 *
 * @see <a href="https://api-docs.igdb.com/#company">IGDB Company Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbCompanyDto(
        Long id,
        String name,
        String slug,
        String description,

        @JsonProperty("start_date")
        Long startDate,

        IgdbCompanyLogoDto logo,

        String url,
        Integer country
) {}
