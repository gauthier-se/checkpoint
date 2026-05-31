package com.checkpoint.api.controllers;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.GameDetailDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.list.GameListCardDto;
import com.checkpoint.api.services.GameCatalogService;
import com.checkpoint.api.services.GameListService;
import com.checkpoint.api.services.GameSearchService;
import com.checkpoint.api.services.GameSimilarityService;
import com.checkpoint.api.services.GameTrendingService;

/**
 * REST controller for public game catalog endpoints.
 * Provides paginated access to the game catalog and game details.
 */
@Tag(name = "Games", description = "Game catalog: search, detail, trending and discovery")
@RestController
@RequestMapping("/games")
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_TRENDING_SIZE = 7;
    private static final int MAX_TRENDING_SIZE = 20;
    private static final int DEFAULT_DISCOVERY_SIZE = 7;
    private static final int MAX_DISCOVERY_SIZE = 20;
    private static final int DEFAULT_LISTS_SIZE = 6;
    private static final int MAX_LISTS_SIZE = 50;
    private static final int DEFAULT_SIMILAR_SIZE = 12;
    private static final int MAX_SIMILAR_SIZE = 30;
    private static final String DEFAULT_SORT = "releaseDate,desc";

    private final GameCatalogService gameCatalogService;
    private final GameSearchService gameSearchService;
    private final GameTrendingService gameTrendingService;
    private final GameListService gameListService;
    private final GameSimilarityService gameSimilarityService;

    public GameController(GameCatalogService gameCatalogService,
                          GameSearchService gameSearchService,
                          GameTrendingService gameTrendingService,
                          GameListService gameListService,
                          GameSimilarityService gameSimilarityService) {
        this.gameCatalogService = gameCatalogService;
        this.gameSearchService = gameSearchService;
        this.gameTrendingService = gameTrendingService;
        this.gameListService = gameListService;
        this.gameSimilarityService = gameSimilarityService;
    }

    /**
     * Retrieves a paginated list of games with optional filters.
     *
     * @param page      the page number (0-based, default 0)
     * @param size      the page size (default 20, max 100)
     * @param sort      the sort criteria (e.g., "releaseDate,desc" or "title,asc")
     * @param genre     optional genre name filters, repeatable (case-insensitive; matches any)
     * @param platform  optional platform name filters, repeatable (case-insensitive; matches any)
     * @param yearMin   optional minimum release year (inclusive)
     * @param yearMax   optional maximum release year (inclusive)
     * @param ratingMin optional minimum average rating (inclusive)
     * @param ratingMax optional maximum average rating (inclusive)
     * @return paginated list of game cards matching the filters
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<GameCardDto>> getGames(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort,
            @RequestParam(required = false) List<String> genre,
            @RequestParam(required = false) List<String> platform,
            @RequestParam(required = false) Integer yearMin,
            @RequestParam(required = false) Integer yearMax,
            @RequestParam(required = false) Double ratingMin,
            @RequestParam(required = false) Double ratingMax) {

        log.info("GET /api/v1/games - page: {}, size: {}, sort: {}, genre: {}, platform: {}, "
                + "yearMin: {}, yearMax: {}, ratingMin: {}, ratingMax: {}",
                page, size, sort, genre, platform, yearMin, yearMax, ratingMin, ratingMax);

        // Validate and sanitize inputs
        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<GameCardDto> gamePage = gameCatalogService.getGameCatalog(
                pageable, genre, platform, yearMin, yearMax, ratingMin, ratingMax);

        return ResponseEntity.ok(PagedResponseDto.from(gamePage));
    }

    /**
     * Searches for games using full-text search with fuzzy matching.
     * Results are sorted by relevance score.
     *
     * @param q        the search query
     * @param genre    optional genre filter
     * @param platform optional platform filter
     * @return list of matching games sorted by relevance
     */
    @GetMapping("/search")
    public ResponseEntity<List<GameCardDto>> searchGames(
            @RequestParam String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String platform) {

        log.info("GET /api/v1/games/search - q: '{}', genre: '{}', platform: '{}'", q, genre, platform);

        List<GameCardDto> results = gameSearchService.searchGames(q, genre, platform);
        return ResponseEntity.ok(results);
    }

    /**
     * Returns trending games based on recent user activity.
     * Games are scored by weighted recent library additions, play sessions,
     * ratings, reviews, likes, and wishlist additions from the last 7 days.
     *
     * @param size the number of trending games to return (default 7, max 20)
     * @return a list of trending game cards
     */
    @GetMapping("/trending")
    public ResponseEntity<List<GameCardDto>> getTrendingGames(
            @RequestParam(defaultValue = "" + DEFAULT_TRENDING_SIZE) int size) {

        int validatedSize = Math.min(Math.max(1, size), MAX_TRENDING_SIZE);
        log.info("GET /api/v1/games/trending - size: {}", validatedSize);

        List<GameCardDto> trending = gameTrendingService.getTrendingGames(validatedSize);
        return ResponseEntity.ok(trending);
    }

    /**
     * Returns the games appearing in the most users' backlogs, ranked by descending count.
     *
     * @param size the number of games to return (default 7, max 20)
     * @return a list of game cards ordered by backlog count (descending)
     */
    @GetMapping("/most-backlogged")
    public ResponseEntity<List<GameCardDto>> getMostBackloggedGames(
            @RequestParam(defaultValue = "" + DEFAULT_DISCOVERY_SIZE) int size) {

        int validatedSize = Math.min(Math.max(1, size), MAX_DISCOVERY_SIZE);
        log.info("GET /api/v1/games/most-backlogged - size: {}", validatedSize);

        return ResponseEntity.ok(gameCatalogService.getMostBackloggedGames(validatedSize));
    }

    /**
     * Returns the games appearing in the most users' wishlists, ranked by descending count.
     *
     * @param size the number of games to return (default 7, max 20)
     * @return a list of game cards ordered by wishlist count (descending)
     */
    @GetMapping("/most-wishlisted")
    public ResponseEntity<List<GameCardDto>> getMostWishlistedGames(
            @RequestParam(defaultValue = "" + DEFAULT_DISCOVERY_SIZE) int size) {

        int validatedSize = Math.min(Math.max(1, size), MAX_DISCOVERY_SIZE);
        log.info("GET /api/v1/games/most-wishlisted - size: {}", validatedSize);

        return ResponseEntity.ok(gameCatalogService.getMostWishlistedGames(validatedSize));
    }

    /**
     * Retrieves detailed information about a specific game.
     *
     * @param id the game ID
     * @return game details
     */
    @GetMapping("/{id}")
    public ResponseEntity<GameDetailDto> getGameById(@PathVariable UUID id) {
        log.info("GET /api/v1/games/{}", id);

        GameDetailDto game = gameCatalogService.getGameDetails(id);
        return ResponseEntity.ok(game);
    }

    /**
     * Returns the lists in which a given game appears, with visibility filtering.
     * Anonymous viewers see only public lists. Authenticated viewers additionally
     * see their own private lists. Ordered by list popularity (likes desc),
     * with the game's inclusion date (addedAt desc) as a tiebreaker.
     *
     * @param gameId      the game ID
     * @param page        0-based page number
     * @param size        page size (default 6, max 50)
     * @param userDetails the authenticated user, or null if anonymous
     * @return paginated list of game list card DTOs
     */
    @GetMapping("/{gameId}/lists")
    public ResponseEntity<PagedResponseDto<GameListCardDto>> getListsContainingGame(
            @PathVariable UUID gameId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_LISTS_SIZE) int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        log.info("GET /api/v1/games/{}/lists - page: {}, size: {}, viewer: {}",
                gameId, page, size, viewerEmail != null ? viewerEmail : "anonymous");

        int validatedSize = Math.min(Math.max(1, size), MAX_LISTS_SIZE);
        int validatedPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        Page<GameListCardDto> lists = gameListService.findListsContainingGame(gameId, viewerEmail, pageable);
        return ResponseEntity.ok(PagedResponseDto.from(lists));
    }

    /**
     * Returns games similar to the given game, ranked by shared genre / company / platform
     * overlap. The result is item-to-item (driven by the seed game, not the viewer's
     * library), but when the viewer is authenticated, games already in their library,
     * wishlist, favorites, or likes are excluded. Anonymous viewers get the unfiltered list.
     *
     * @param gameId      the seed game ID
     * @param size        the number of similar games to return (default 12, max 30)
     * @param userDetails the authenticated user, or null if anonymous
     * @return a list of similar game cards, best match first (may be empty)
     */
    @GetMapping("/{gameId}/similar")
    public ResponseEntity<List<GameCardDto>> getSimilarGames(
            @PathVariable UUID gameId,
            @RequestParam(defaultValue = "" + DEFAULT_SIMILAR_SIZE) int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        log.info("GET /api/v1/games/{}/similar - size: {}, viewer: {}",
                gameId, size, viewerEmail != null ? viewerEmail : "anonymous");

        int validatedSize = Math.min(Math.max(1, size), MAX_SIMILAR_SIZE);

        List<GameCardDto> similar = gameSimilarityService.getSimilarGames(gameId, viewerEmail, validatedSize);
        return ResponseEntity.ok(similar);
    }

    /**
     * Creates a Pageable from the sort string.
     * Supports format: "field,direction" (e.g., "releaseDate,desc")
     */
    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1
                && sortParts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // Map common sort fields to entity fields
        String mappedField = mapSortField(sortField);

        return PageRequest.of(page, size, Sort.by(direction, mappedField));
    }

    /**
     * Maps API sort field names to entity field names.
     */
    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "releasedate", "release_date" -> "releaseDate";
            case "title", "name" -> "title";
            case "rating" -> "averageRating"; // Note: requires special handling in query
            case "createdat", "created_at" -> "createdAt";
            default -> "releaseDate";
        };
    }
}
