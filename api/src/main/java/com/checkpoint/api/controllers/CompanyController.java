package com.checkpoint.api.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.CompanyCatalogDto;
import com.checkpoint.api.services.CompanyCatalogService;

/**
 * REST controller for public company catalog endpoints.
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyCatalogService companyCatalogService;

    public CompanyController(CompanyCatalogService companyCatalogService) {
        this.companyCatalogService = companyCatalogService;
    }

    /**
     * Retrieves all companies sorted alphabetically by name.
     */
    @GetMapping
    public ResponseEntity<List<CompanyCatalogDto>> getAllCompanies() {
        log.info("GET /api/companies");

        List<CompanyCatalogDto> companies = companyCatalogService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }
}
