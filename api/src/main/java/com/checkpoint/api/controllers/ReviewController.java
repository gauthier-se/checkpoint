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
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.catalog.ReviewCardDto;
import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.services.ReviewService;

/**
 * REST controller for public review endpoints.
 *
 * <p>Provides:
 * <ul>
 *   <li>Paginated reviews for a specific game ({@code /api/games/{gameId}/reviews})</li>
 *   <li>Cross-game discovery feeds: popular and recent reviews
 *       ({@code /api/reviews/popular}, {@code /api/reviews/recent})</li>
 * </ul>
 * Reviews are created, updated, and deleted via play log endpoints
 * ({@link PlayLogReviewController}).</p>
 */
@RestController
@RequestMapping("/api")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt,desc";

    private static final int DEFAULT_DISCOVERY_SIZE = 7;
    private static final int MAX_DISCOVERY_SIZE = 20;

    private final ReviewService reviewService;

    /**
     * Constructs a new ReviewController.
     *
     * @param reviewService the review service
     */
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Retrieves a paginated list of all reviews for a specific game.
     * Includes reviews from all users and all their play logs, ordered by date.
     * Accessible to both public and authenticated users.
     * When authenticated, the response includes whether the viewer has liked each review.
     *
     * @param gameId      the video game ID
     * @param userDetails the authenticated user, or null if anonymous
     * @param page        the page number (0-based)
     * @param size        the page size
     * @param sort        the sorting parameters
     * @return the paginated reviews with play context
     */
    @GetMapping("/games/{gameId}/reviews")
    public ResponseEntity<PagedResponseDto<ReviewResponseDto>> getReviews(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/games/{}/reviews - page: {}, size: {}, sort: {}, viewer: {}",
                gameId, page, size, sort,
                userDetails != null ? userDetails.getUsername() : "anonymous");

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<ReviewResponseDto> reviewPage = reviewService.getGameReviews(gameId, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(reviewPage));
    }

    /**
     * Returns the top reviews ranked by a "hot" score combining likes and recency.
     *
     * @param userDetails the authenticated user, or null if anonymous
     * @param size        the number of reviews to return (default 7, max 20)
     * @return the popular reviews with game and play context
     */
    @GetMapping("/reviews/popular")
    public ResponseEntity<List<ReviewCardDto>> getPopularReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_DISCOVERY_SIZE) int size) {

        int validatedSize = Math.min(Math.max(1, size), MAX_DISCOVERY_SIZE);
        log.info("GET /api/reviews/popular - size: {}, viewer: {}",
                validatedSize, userDetails != null ? userDetails.getUsername() : "anonymous");

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(reviewService.getPopularReviews(validatedSize, viewerEmail));
    }

    /**
     * Returns the top reviews for a specific game ranked by the same "hot" score
     * used by {@link #getPopularReviews(UserDetails, int)}.
     *
     * @param gameId      the video game ID
     * @param userDetails the authenticated user, or null if anonymous
     * @param size        the number of reviews to return (default 7, max 20)
     * @return the popular reviews for the game
     */
    @GetMapping("/games/{gameId}/reviews/popular")
    public ResponseEntity<List<ReviewCardDto>> getPopularGameReviews(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_DISCOVERY_SIZE) int size) {

        int validatedSize = Math.min(Math.max(1, size), MAX_DISCOVERY_SIZE);
        log.info("GET /api/games/{}/reviews/popular - size: {}, viewer: {}",
                gameId, validatedSize, userDetails != null ? userDetails.getUsername() : "anonymous");

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(reviewService.getPopularGameReviews(gameId, validatedSize, viewerEmail));
    }

    /**
     * Returns reviews authored by the viewer's followings for a specific game.
     * When the viewer is anonymous or follows nobody, the response is an empty page.
     *
     * @param gameId      the video game ID
     * @param userDetails the authenticated user, or null if anonymous
     * @param page        the page number (0-based)
     * @param size        the page size
     * @return the paginated friend reviews
     */
    @GetMapping("/games/{gameId}/reviews/from-friends")
    public ResponseEntity<PagedResponseDto<ReviewResponseDto>> getFriendReviewsForGame(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        log.info("GET /api/games/{}/reviews/from-friends - page: {}, size: {}, viewer: {}",
                gameId, validatedPage, validatedSize,
                userDetails != null ? userDetails.getUsername() : "anonymous");

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        Pageable pageable = PageRequest.of(validatedPage, validatedSize);
        Page<ReviewResponseDto> reviewPage = reviewService.getFriendReviewsForGame(gameId, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(reviewPage));
    }

    /**
     * Returns the most recently created reviews across all games and users.
     *
     * @param userDetails the authenticated user, or null if anonymous
     * @param size        the number of reviews to return (default 7, max 20)
     * @return the recent reviews with game and play context
     */
    @GetMapping("/reviews/recent")
    public ResponseEntity<List<ReviewCardDto>> getRecentReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_DISCOVERY_SIZE) int size) {

        int validatedSize = Math.min(Math.max(1, size), MAX_DISCOVERY_SIZE);
        log.info("GET /api/reviews/recent - size: {}, viewer: {}",
                validatedSize, userDetails != null ? userDetails.getUsername() : "anonymous");

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(reviewService.getRecentReviews(validatedSize, viewerEmail));
    }

    /**
     * Creates a Pageable from the sort string.
     * Supports format: "field,direction" (e.g., "createdAt,desc").
     *
     * @param page the page number
     * @param size the page size
     * @param sort the sort string
     * @return a Pageable instance
     */
    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1
                && sortParts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String mappedField = mapSortField(sortField);

        return PageRequest.of(page, size, Sort.by(direction, mappedField));
    }

    /**
     * Maps API sort field names to entity field names.
     *
     * @param field the API field name
     * @return the entity field name
     */
    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "createdAt";
        };
    }
}
