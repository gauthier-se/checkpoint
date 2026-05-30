package com.checkpoint.api.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.collection.UserGameRequestDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.services.UserGameCollectionService;

import jakarta.validation.Valid;

/**
 * REST controller for the authenticated user's game collection (library).
 *
 * <p>All endpoints require authentication (JWT or session).
 * The authenticated user is resolved from the security context.</p>
 */
@Tag(name = "Library and Collection", description = "Current user game library")
@RestController
@RequestMapping("/api/me/library")
public class UserGameCollectionController {

    private static final Logger log = LoggerFactory.getLogger(UserGameCollectionController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt,desc";

    private final UserGameCollectionService userGameCollectionService;

    public UserGameCollectionController(UserGameCollectionService userGameCollectionService) {
        this.userGameCollectionService = userGameCollectionService;
    }

    /**
     * Adds a game to the authenticated user's library with a given status.
     *
     * @param userDetails the authenticated user principal
     * @param request     the request containing video game ID and status
     * @return the created user-game entry with 201 status
     */
    @PostMapping
    public ResponseEntity<UserGameResponseDto> addGameToLibrary(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserGameRequestDto request) {

        log.info("POST /api/me/library - user: {}, gameId: {}, status: {}",
                userDetails.getUsername(), request.videoGameId(), request.status());

        UserGameResponseDto response = userGameCollectionService.addGameToLibrary(
                userDetails.getUsername(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the status of a game in the authenticated user's library.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the video game ID
     * @param request     the request containing the new status
     * @return the updated user-game entry
     */
    @PutMapping("/{videoGameId}")
    public ResponseEntity<UserGameResponseDto> updateGameStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId,
            @Valid @RequestBody UserGameRequestDto request) {

        log.info("PUT /api/me/library/{} - user: {}, status: {}",
                videoGameId, userDetails.getUsername(), request.status());

        UserGameResponseDto response = userGameCollectionService.updateGameStatus(
                userDetails.getUsername(), videoGameId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the authenticated user's game collection (paginated).
     *
     * @param userDetails the authenticated user principal
     * @param status      optional status filter (PLAYING, COMPLETED, DROPPED); null = all statuses
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @param sort        the sort criteria (e.g., "createdAt,desc" or "rating,desc")
     * @return paginated list of games in the user's library
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<UserGameResponseDto>> getUserLibrary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PlayStatus status,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/me/library - user: {}, status: {}, page: {}, size: {}, sort: {}",
                userDetails.getUsername(), status, page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<UserGameResponseDto> libraryPage = userGameCollectionService.getUserLibrary(
                userDetails.getUsername(), status, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(libraryPage));
    }

    /**
     * Removes a game from the authenticated user's library.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the video game ID to remove
     * @return 204 No Content
     */
    @DeleteMapping("/{videoGameId}")
    public ResponseEntity<Void> removeGameFromLibrary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId) {

        log.info("DELETE /api/me/library/{} - user: {}", videoGameId, userDetails.getUsername());

        userGameCollectionService.removeGameFromLibrary(userDetails.getUsername(), videoGameId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a Pageable from the sort string.
     * Supports format: "field,direction" (e.g., "createdAt,desc")
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
     * Maps API sort field names to entity field names. The {@code rating} field is a
     * sentinel — the service detects it on the resulting Sort and routes the call to
     * the rating-sorted repository query (which expresses the ORDER BY directly in JPQL).
     */
    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "createdat", "created_at", "addedat", "added_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            case "status" -> "status";
            case "title", "name" -> "videoGame.title";
            case "rating" -> "rating";
            default -> "createdAt";
        };
    }
}
