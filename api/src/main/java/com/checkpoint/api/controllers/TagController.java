package com.checkpoint.api.controllers;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.playlog.GamePlayLogResponseDto;
import com.checkpoint.api.dto.tag.TagRequestDto;
import com.checkpoint.api.dto.tag.TagResponseDto;
import com.checkpoint.api.services.TagService;

import jakarta.validation.Valid;

/**
 * REST controller for user-scoped tag management.
 * Handles both authenticated (/api/me/tags) and public (/api/users/{username}/tags) endpoints.
 */
@Tag(name = "Gamification", description = "Game tags")
@RestController
public class TagController {

    private static final Logger log = LoggerFactory.getLogger(TagController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "updatedAt,desc";

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * Creates a new tag for the authenticated user.
     *
     * @param userDetails the authenticated user
     * @param request     the tag creation request
     * @return the created tag
     */
    @PostMapping("/api/me/tags")
    public ResponseEntity<TagResponseDto> createTag(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TagRequestDto request) {

        log.info("POST /api/me/tags - user: {}", userDetails.getUsername());
        TagResponseDto response = tagService.createTag(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all tags belonging to the authenticated user with play log counts.
     *
     * @param userDetails the authenticated user
     * @return list of tags
     */
    @GetMapping("/api/me/tags")
    public ResponseEntity<List<TagResponseDto>> getUserTags(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/me/tags - user: {}", userDetails.getUsername());
        List<TagResponseDto> tags = tagService.getUserTags(userDetails.getUsername());
        return ResponseEntity.ok(tags);
    }

    /**
     * Renames an existing tag.
     *
     * @param userDetails the authenticated user
     * @param tagId       the tag ID to rename
     * @param request     the rename request
     * @return the updated tag
     */
    @PutMapping("/api/me/tags/{tagId}")
    public ResponseEntity<TagResponseDto> updateTag(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID tagId,
            @Valid @RequestBody TagRequestDto request) {

        log.info("PUT /api/me/tags/{} - user: {}", tagId, userDetails.getUsername());
        TagResponseDto response = tagService.updateTag(userDetails.getUsername(), tagId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a tag and removes all play log associations.
     *
     * @param userDetails the authenticated user
     * @param tagId       the tag ID to delete
     * @return 204 No Content
     */
    @DeleteMapping("/api/me/tags/{tagId}")
    public ResponseEntity<Void> deleteTag(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID tagId) {

        log.info("DELETE /api/me/tags/{} - user: {}", tagId, userDetails.getUsername());
        tagService.deleteTag(userDetails.getUsername(), tagId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns paginated play logs associated with a specific tag (authenticated).
     *
     * @param userDetails the authenticated user
     * @param tagId       the tag ID
     * @param page        the page number (0-based)
     * @param size        the page size
     * @param sort        the sort field and direction
     * @return paginated play logs
     */
    @GetMapping("/api/me/tags/{tagId}/plays")
    public ResponseEntity<PagedResponseDto<GamePlayLogResponseDto>> getPlayLogsByTag(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {

        log.info("GET /api/me/tags/{}/plays - user: {}", tagId, userDetails.getUsername());

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);
        Pageable pageable = createPageable(validatedPage, validatedSize, sort);

        Page<GamePlayLogResponseDto> result = tagService.getPlayLogsByTag(
                userDetails.getUsername(), tagId, pageable);
        return ResponseEntity.ok(PagedResponseDto.from(result));
    }

    /**
     * Returns all tags belonging to a user by username (public).
     *
     * @param username the target user's username
     * @return list of tags with counts
     */
    @GetMapping("/api/users/{username}/tags")
    public ResponseEntity<List<TagResponseDto>> getPublicUserTags(
            @PathVariable String username) {

        log.info("GET /api/users/{}/tags", username);
        List<TagResponseDto> tags = tagService.getPublicUserTags(username);
        return ResponseEntity.ok(tags);
    }

    /**
     * Returns paginated games for a user's tag by name (public).
     *
     * @param username the target user's username
     * @param tagName  the tag name
     * @param page     the page number (0-based)
     * @param size     the page size
     * @param sort     the sort field and direction
     * @return paginated play logs
     */
    @GetMapping("/api/users/{username}/tags/{tagName}/games")
    public ResponseEntity<PagedResponseDto<GamePlayLogResponseDto>> getPublicPlayLogsByTag(
            @PathVariable String username,
            @PathVariable String tagName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {

        log.info("GET /api/users/{}/tags/{}/games", username, tagName);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);
        Pageable pageable = createPageable(validatedPage, validatedSize, sort);

        Page<GamePlayLogResponseDto> result = tagService.getPublicPlayLogsByTag(
                username, tagName, pageable);
        return ResponseEntity.ok(PagedResponseDto.from(result));
    }

    /**
     * Creates a Pageable from page, size, and sort parameters.
     *
     * @param page the page number
     * @param size the page size
     * @param sort the sort string (e.g. "updatedAt,desc")
     * @return the Pageable instance
     */
    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }
}
