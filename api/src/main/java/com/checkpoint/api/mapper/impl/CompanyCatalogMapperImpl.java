package com.checkpoint.api.mapper.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.checkpoint.api.dto.catalog.CompanyCatalogDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.mapper.CompanyCatalogMapper;

/**
 * Implementation of {@link CompanyCatalogMapper}.
 */
@Component
public class CompanyCatalogMapperImpl implements CompanyCatalogMapper {

    @Override
    public CompanyCatalogDto toDto(Company company) {
        return new CompanyCatalogDto(
                company.getId(),
                company.getName(),
                company.getVideoGamesCount()
        );
    }

    @Override
    public List<CompanyCatalogDto> toDtoList(List<Company> companies) {
        return companies.stream()
                .map(this::toDto)
                .toList();
    }
}
