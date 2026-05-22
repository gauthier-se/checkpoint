package com.checkpoint.api.mapper;

import java.util.List;

import com.checkpoint.api.dto.catalog.CompanyCatalogDto;
import com.checkpoint.api.entities.Company;

/**
 * Mapper for converting {@link Company} entities to catalog DTOs.
 */
public interface CompanyCatalogMapper {

    CompanyCatalogDto toDto(Company company);

    List<CompanyCatalogDto> toDtoList(List<Company> companies);
}
