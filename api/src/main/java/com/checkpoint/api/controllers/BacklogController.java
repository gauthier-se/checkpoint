package com.checkpoint.api.controllers;

import java.util.Map;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.collection.BacklogResponseDto;
import com.checkpoint.api.dto.collection.UpdatePriorityRequestDto;
import com.checkpoint.api.enums.Priority;
import com.checkpoint.api.services.BacklogService;

/**
 * REST controller for the authenticated user's backlog.
 *
 * <p>All endpoints require authentication (JWT or session).
 * The authenticated user is resolved from the security context.</p>
 */
@Tag(name = "Library and Collection", description = "Current user backlog")
@RestController
@RequestMapping("/api/me/backlog")
public class BacklogController {

    private static final Logger log = LoggerFactory.getLogger(BacklogController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt,desc";

    private final BacklogService backlogService;

    public BacklogController(BacklogService backlogService) {
        this.backlogService = backlogService;
    }

    /**
     * Adds a game to the authenticated user's backlog.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the video game ID to add
     * @return the created backlog entry with 201 status
     */
    @PostMapping("/{videoGameId}")
    public ResponseEntity<BacklogResponseDto> addToBacklog(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId,
            @RequestBody(required = false) UpdatePriorityRequestDto request) {

        Priority priority = request != null ? request.priority() : null;
        log.info("POST /api/me/backlog/{} - user: {}, priority: {}",
                videoGameId, userDetails.getUsername(), priority);

        BacklogResponseDto response = backlogService.addToBacklog(
                userDetails.getUsername(), videoGameId, priority);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Removes a game from the authenticated user's backlog.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the video game ID to remove
     * @return 204 No Content
     */
    @DeleteMapping("/{videoGameId}")
    public ResponseEntity<Void> removeFromBacklog(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId) {

        log.info("DELETE /api/me/backlog/{} - user: {}", videoGameId, userDetails.getUsername());

        backlogService.removeFromBacklog(userDetails.getUsername(), videoGameId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the authenticated user's backlog (paginated).
     *
     * @param userDetails the authenticated user principal
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @param sort        the sort criteria (e.g., "createdAt,desc")
     * @return paginated list of backlog games
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<BacklogResponseDto>> getUserBacklog(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/me/backlog - user: {}, page: {}, size: {}, sort: {}",
                userDetails.getUsername(), page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<BacklogResponseDto> backlogPage = backlogService.getUserBacklog(
                userDetails.getUsername(), pageable);

        return ResponseEntity.ok(PagedResponseDto.from(backlogPage));
    }

    /**
     * Sets or clears the priority of a game in the authenticated user's backlog.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the video game ID
     * @param request     the priority update payload (priority may be {@code null} to clear)
     * @return the updated backlog entry with 200 status
     */
    @PatchMapping("/{videoGameId}/priority")
    public ResponseEntity<BacklogResponseDto> updatePriority(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId,
            @RequestBody UpdatePriorityRequestDto request) {

        log.info("PATCH /api/me/backlog/{}/priority - user: {}, priority: {}",
                videoGameId, userDetails.getUsername(), request.priority());

        BacklogResponseDto response = backlogService.updatePriority(
                userDetails.getUsername(), videoGameId, request.priority());

        return ResponseEntity.ok(response);
    }

    /**
     * Checks if a game is in the authenticated user's backlog.
     *
     * @param userDetails the authenticated user principal
     * @param videoGameId the video game ID to check
     * @return JSON with "inBacklog" boolean
     */
    @GetMapping("/{videoGameId}/status")
    public ResponseEntity<Map<String, Boolean>> isInBacklog(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID videoGameId) {

        log.info("GET /api/me/backlog/{}/status - user: {}", videoGameId, userDetails.getUsername());

        boolean inBacklog = backlogService.isInBacklog(
                userDetails.getUsername(), videoGameId);

        return ResponseEntity.ok(Map.of("inBacklog", inBacklog));
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
     * Maps API sort field names to entity field names.
     */
    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "createdat", "created_at", "addedat", "added_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            case "title", "name" -> "videoGame.title";
            case "priority" -> "priority";
            default -> "createdAt";
        };
    }
}
