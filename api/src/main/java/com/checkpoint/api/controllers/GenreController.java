package com.checkpoint.api.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.GenreCatalogDto;
import com.checkpoint.api.services.GenreCatalogService;

/**
 * REST controller for public genre catalog endpoints.
 * Provides access to the list of genres with their video game counts.
 */
@Tag(name = "Games", description = "Game genres catalog")
@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private static final Logger log = LoggerFactory.getLogger(GenreController.class);

    private final GenreCatalogService genreCatalogService;

    public GenreController(GenreCatalogService genreCatalogService) {
        this.genreCatalogService = genreCatalogService;
    }

    /**
     * Retrieves all genres sorted alphabetically by name.
     *
     * @return list of genres with their video game counts
     */
    @GetMapping
    public ResponseEntity<List<GenreCatalogDto>> getAllGenres() {
        log.info("GET /api/genres");

        List<GenreCatalogDto> genres = genreCatalogService.getAllGenres();
        return ResponseEntity.ok(genres);
    }
}
