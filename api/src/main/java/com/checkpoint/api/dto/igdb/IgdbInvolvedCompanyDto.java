package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for IGDB Involved Company response.
 * Represents a company's involvement in a game (developer, publisher, etc.).
 *
 * @see <a href="https://api-docs.igdb.com/#involved-company">IGDB Involved Company Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbInvolvedCompanyDto(
        Long id,
        IgdbCompanyDto company,
        Boolean developer,
        Boolean publisher,
        Boolean porting,
        Boolean supporting
) {}
