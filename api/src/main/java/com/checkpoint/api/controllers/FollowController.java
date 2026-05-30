package com.checkpoint.api.controllers;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.social.FollowResponseDto;
import com.checkpoint.api.dto.social.FollowUserDto;
import com.checkpoint.api.services.FollowService;

/**
 * REST controller for the social graph (follow system).
 *
 * <p>The toggle follow endpoint requires authentication.
 * The followers/following list endpoints are public.</p>
 */
@Tag(name = "Account and Profile", description = "Following and followers")
@RestController
@RequestMapping("/api/users")
public class FollowController {

    private static final Logger log = LoggerFactory.getLogger(FollowController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "pseudo,asc";

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /**
     * Toggles the follow status between the authenticated user and the target user.
     * If the user is already following the target, they will unfollow. Otherwise, they will follow.
     *
     * @param userDetails  the authenticated user principal
     * @param userId       the target user's ID
     * @return the new follow status
     */
    @PostMapping("/{userId}/follow")
    public ResponseEntity<FollowResponseDto> toggleFollow(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId) {

        log.info("POST /api/users/{}/follow - user: {}", userId, userDetails.getUsername());

        FollowResponseDto response = followService.toggleFollow(
                userDetails.getUsername(), userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns a paginated list of users who follow the given user.
     *
     * @param userId the user's ID
     * @param page   the page number (0-based, default 0)
     * @param size   the page size (default 20, max 100)
     * @param sort   the sort criteria (e.g., "pseudo,asc")
     * @return paginated list of follower user DTOs
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<PagedResponseDto<FollowUserDto>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/users/{}/followers - page: {}, size: {}, sort: {}", userId, page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<FollowUserDto> followersPage = followService.getFollowers(userId, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(followersPage));
    }

    /**
     * Returns a paginated list of users that the given user follows.
     *
     * @param userId the user's ID
     * @param page   the page number (0-based, default 0)
     * @param size   the page size (default 20, max 100)
     * @param sort   the sort criteria (e.g., "pseudo,asc")
     * @return paginated list of following user DTOs
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<PagedResponseDto<FollowUserDto>> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/users/{}/following - page: {}, size: {}, sort: {}", userId, page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<FollowUserDto> followingPage = followService.getFollowing(userId, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(followingPage));
    }

    /**
     * Removes a follower from the authenticated user. The given follower will no
     * longer follow the authenticated user. Idempotent and silent — no notification
     * is sent to the removed follower.
     *
     * @param userDetails the authenticated user principal
     * @param followerId  the ID of the follower to remove
     * @return 204 No Content
     */
    @DeleteMapping("/me/followers/{followerId}")
    public ResponseEntity<Void> removeFollower(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID followerId) {

        log.info("DELETE /api/users/me/followers/{} - user: {}", followerId, userDetails.getUsername());

        followService.removeFollower(userDetails.getUsername(), followerId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a Pageable from the sort string.
     * Supports format: "field,direction" (e.g., "pseudo,asc")
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
            case "pseudo", "username", "name" -> "pseudo";
            case "createdat", "created_at" -> "createdAt";
            default -> "pseudo";
        };
    }
}
