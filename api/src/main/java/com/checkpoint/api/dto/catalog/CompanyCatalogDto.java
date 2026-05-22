package com.checkpoint.api.dto.catalog;

import java.util.UUID;

/**
 * DTO for company catalog listing.
 *
 * @param id              the company ID
 * @param name            the company name
 * @param videoGamesCount the number of video games created by this company
 */
public record CompanyCatalogDto(
        UUID id,
        String name,
        Integer videoGamesCount
) {}
