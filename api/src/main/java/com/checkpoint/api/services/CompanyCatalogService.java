package com.checkpoint.api.services;

import java.util.List;

import com.checkpoint.api.dto.catalog.CompanyCatalogDto;

/**
 * Service interface for company catalog operations.
 * Provides methods for retrieving companies for public display
 * (and to populate admin pickers).
 */
public interface CompanyCatalogService {

    /**
     * Retrieves all companies sorted alphabetically by name.
     */
    List<CompanyCatalogDto> getAllCompanies();
}
