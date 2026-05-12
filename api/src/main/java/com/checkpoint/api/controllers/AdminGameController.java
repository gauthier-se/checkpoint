package com.checkpoint.api.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.admin.BulkImportResultDto;
import com.checkpoint.api.dto.admin.ExternalGameDto;
import com.checkpoint.api.dto.catalog.GameDetailDto;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.services.AdminGameService;

/**
 * REST controller for admin game management operations.
 * Provides endpoints for searching and importing games from external APIs.
 *
 * Note: These endpoints should be secured with ROLE_ADMIN when Spring Security is configured.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminGameController {

    private static final Logger log = LoggerFactory.getLogger(AdminGameController.class);
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 50;
    private static final int MAX_BULK_LIMIT = 500;

    private final AdminGameService adminGameService;

    public AdminGameController(AdminGameService adminGameService) {
        this.adminGameService = adminGameService;
    }

    /**
     * Search for games in the external API (IGDB).
     *
     * @param query the search query
     * @param limit maximum number of results (default: 20, max: 50)
     * @return list of matching external games
     */
    @GetMapping("/external-games/search")
    public ResponseEntity<List<ExternalGameDto>> searchExternalGames(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("Admin search request: query='{}', limit={}", query, limit);

        // Validate and cap the limit
        int effectiveLimit = Math.min(Math.max(1, limit), MAX_SEARCH_LIMIT);
        if (effectiveLimit != limit) {
            log.debug("Adjusted search limit from {} to {}", limit, effectiveLimit);
        }

        List<ExternalGameDto> results = adminGameService.searchExternalGames(query, effectiveLimit);

        log.info("Search returned {} results for query: '{}'", results.size(), query);
        return ResponseEntity.ok(results);
    }

    /**
     * Import a game from the external API (IGDB) by its external ID.
     * If the game already exists in the database, it will be updated.
     *
     * @param externalId the IGDB game ID
     * @return the imported/updated game details
     */
    @PostMapping("/games/import/{externalId}")
    public ResponseEntity<GameDetailDto> importGame(@PathVariable Long externalId) {

        log.info("Admin import request for external ID: {}", externalId);

        VideoGame importedGame = adminGameService.importGameByExternalId(externalId);

        GameDetailDto response = mapToGameDetailDto(importedGame);

        log.info("Successfully imported game: {} (internal ID: {})", importedGame.getTitle(), importedGame.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Bulk-imports the top-rated games from IGDB. Already-imported games are
     * skipped (deduplication by IGDB ID). Runs synchronously and may take
     * several minutes for large batches due to IGDB rate limiting.
     *
     * @param limit          number of games to fetch (default 100, max 500)
     * @param minRatingCount minimum IGDB rating count to qualify (default 100)
     * @return summary of the operation
     */
    @PostMapping("/games/import/top-rated")
    public ResponseEntity<BulkImportResultDto> bulkImportTopRated(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "100") int minRatingCount) {

        int effectiveLimit = Math.min(Math.max(1, limit), MAX_BULK_LIMIT);
        int effectiveMinRating = Math.max(0, minRatingCount);
        log.info("Admin bulk top-rated import request: limit={}, minRatingCount={}",
                effectiveLimit, effectiveMinRating);

        BulkImportResultDto result = adminGameService.bulkImportTopRatedGames(effectiveLimit, effectiveMinRating);

        log.info("Bulk top-rated import done: {} imported, {} skipped, {} failed (of {} fetched)",
                result.imported(), result.skipped(), result.failed(), result.totalFetched());
        return ResponseEntity.ok(result);
    }

    /**
     * Bulk-imports recently released games from IGDB. Already-imported games
     * are skipped (deduplication by IGDB ID). Runs synchronously.
     *
     * @param limit number of games to fetch (default 100, max 500)
     * @return summary of the operation
     */
    @PostMapping("/games/import/recent")
    public ResponseEntity<BulkImportResultDto> bulkImportRecent(
            @RequestParam(defaultValue = "100") int limit) {

        int effectiveLimit = Math.min(Math.max(1, limit), MAX_BULK_LIMIT);
        log.info("Admin bulk recent import request: limit={}", effectiveLimit);

        BulkImportResultDto result = adminGameService.bulkImportRecentGames(effectiveLimit);

        log.info("Bulk recent import done: {} imported, {} skipped, {} failed (of {} fetched)",
                result.imported(), result.skipped(), result.failed(), result.totalFetched());
        return ResponseEntity.ok(result);
    }

    /**
     * Maps a VideoGame entity to a GameDetailDto for the response.
     */
    private GameDetailDto mapToGameDetailDto(VideoGame game) {
        return new GameDetailDto(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getCoverUrl(),
                game.getArtworkUrl(),
                game.getTrailerYoutubeId(),
                game.getTimeToBeatNormally(),
                game.getTimeToBeatHastily(),
                game.getTimeToBeatCompletely(),
                game.getReleaseDate(),
                null, // averageRating - not available from import
                null, // ratingCount - not available from import
                game.getGenres().stream()
                        .map(genre -> new GameDetailDto.GenreDto(genre.getId(), genre.getName()))
                        .toList(),
                game.getPlatforms().stream()
                        .map(platform -> new GameDetailDto.PlatformDto(platform.getId(), platform.getName()))
                        .toList(),
                game.getCompanies().stream()
                        .map(company -> new GameDetailDto.CompanyDto(company.getId(), company.getName()))
                        .toList()
        );
    }
}

