package com.checkpoint.api.services.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.CompanyCatalogDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.mapper.CompanyCatalogMapper;
import com.checkpoint.api.repositories.CompanyRepository;
import com.checkpoint.api.services.CompanyCatalogService;

/**
 * Implementation of {@link CompanyCatalogService}.
 */
@Service
@Transactional(readOnly = true)
public class CompanyCatalogServiceImpl implements CompanyCatalogService {

    private static final Logger log = LoggerFactory.getLogger(CompanyCatalogServiceImpl.class);

    private final CompanyRepository companyRepository;
    private final CompanyCatalogMapper companyCatalogMapper;

    public CompanyCatalogServiceImpl(CompanyRepository companyRepository,
                                     CompanyCatalogMapper companyCatalogMapper) {
        this.companyRepository = companyRepository;
        this.companyCatalogMapper = companyCatalogMapper;
    }

    @Override
    public List<CompanyCatalogDto> getAllCompanies() {
        log.debug("Fetching all companies sorted by name");

        List<Company> companies = companyRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return companyCatalogMapper.toDtoList(companies);
    }
}
