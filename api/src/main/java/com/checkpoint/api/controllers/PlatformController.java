package com.checkpoint.api.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.PlatformCatalogDto;
import com.checkpoint.api.services.PlatformCatalogService;

/**
 * REST controller for public platform catalog endpoints.
 * Provides access to the list of platforms with their video game counts.
 */
@Tag(name = "Games", description = "Gaming platforms catalog")
@RestController
@RequestMapping("/api/platforms")
public class PlatformController {

    private static final Logger log = LoggerFactory.getLogger(PlatformController.class);

    private final PlatformCatalogService platformCatalogService;

    public PlatformController(PlatformCatalogService platformCatalogService) {
        this.platformCatalogService = platformCatalogService;
    }

    /**
     * Retrieves all platforms sorted alphabetically by name.
     *
     * @return list of platforms with their video game counts
     */
    @GetMapping
    public ResponseEntity<List<PlatformCatalogDto>> getAllPlatforms() {
        log.info("GET /api/platforms");

        List<PlatformCatalogDto> platforms = platformCatalogService.getAllPlatforms();
        return ResponseEntity.ok(platforms);
    }
}
