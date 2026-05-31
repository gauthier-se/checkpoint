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
import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.dto.collection.BacklogResponseDto;
import com.checkpoint.api.dto.collection.LikedGameResponseDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.dto.collection.WishResponseDto;
import com.checkpoint.api.dto.list.GameListCardDto;
import com.checkpoint.api.dto.playlog.GamePlayLogResponseDto;
import com.checkpoint.api.dto.profile.ProfileComparisonDto;
import com.checkpoint.api.dto.profile.UserProfileDto;
import com.checkpoint.api.dto.social.FollowResponseDto;
import com.checkpoint.api.dto.social.FollowUserDto;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.services.FollowService;
import com.checkpoint.api.services.ProfileComparisonService;
import com.checkpoint.api.services.ProfileService;

/**
 * REST controller for public user resources under {@code /users}: public
 * profiles and their tabs (reviews, wishlist, library, backlog, plays, lists),
 * profile comparison, and the follow graph (follow, followers, following).
 *
 * <p>Profile and follow-list endpoints are public; when an authenticated user
 * accesses them, additional context is provided (isFollowing, isOwner) and
 * tab data respects privacy settings. Toggling follow and comparison require
 * authentication (enforced by the security configuration).</p>
 */
@Tag(name = "Account and Profile", description = "Public user profiles, library, plays, backlog, and following")
@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final String DEFAULT_FOLLOW_SORT = "pseudo,asc";

    private final ProfileService profileService;
    private final ProfileComparisonService profileComparisonService;
    private final FollowService followService;

    public UserController(ProfileService profileService,
                          ProfileComparisonService profileComparisonService,
                          FollowService followService) {
        this.profileService = profileService;
        this.profileComparisonService = profileComparisonService;
        this.followService = followService;
    }

    /**
     * Returns the public profile for a user by username.
     *
     * @param username    the user's display name (pseudo)
     * @param userDetails the authenticated user, or null if anonymous
     * @return the user profile DTO
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserProfileDto> getUserProfile(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/users/{} - viewer: {}", username,
                userDetails != null ? userDetails.getUsername() : "anonymous");

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        UserProfileDto profile = profileService.getUserProfile(username, viewerEmail);

        return ResponseEntity.ok(profile);
    }

    /**
     * Returns a paginated list of reviews written by the given user.
     *
     * @param username    the user's display name (pseudo)
     * @param userDetails the authenticated user, or null if anonymous
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @param sort        the sort criteria (e.g., "createdAt,desc")
     * @return paginated list of review DTOs
     */
    @GetMapping("/{username}/reviews")
    public ResponseEntity<PagedResponseDto<ReviewResponseDto>> getUserReviews(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/v1/users/{}/reviews - page: {}, size: {}", username, page, size);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<ReviewResponseDto> reviews = profileService.getUserReviews(username, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(reviews));
    }

    /**
     * Returns a paginated list of wishlist items for the given user.
     *
     * @param username    the user's display name (pseudo)
     * @param userDetails the authenticated user, or null if anonymous
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @param sort        the sort criteria (e.g., "createdAt,desc")
     * @return paginated list of wish DTOs
     */
    @GetMapping("/{username}/wishlist")
    public ResponseEntity<PagedResponseDto<WishResponseDto>> getUserWishlist(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/v1/users/{}/wishlist - page: {}, size: {}", username, page, size);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<WishResponseDto> wishlist = profileService.getUserWishlist(username, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(wishlist));
    }

    /**
     * Returns a paginated list of games the given user has liked.
     *
     * @param username    the user's display name (pseudo)
     * @param userDetails the authenticated user, or null if anonymous
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @param sort        the sort criteria (e.g., "createdAt,desc")
     * @return paginated list of liked game DTOs
     */
    @GetMapping("/{username}/likes")
    public ResponseEntity<PagedResponseDto<LikedGameResponseDto>> getUserLikedGames(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/v1/users/{}/likes - page: {}, size: {}", username, page, size);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<LikedGameResponseDto> likedGames = profileService.getUserLikedGames(
                username, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(likedGames));
    }

    /**
     * Returns a paginated list of games in the given user's library.
     *
     * @param username    the user's display name (pseudo)
     * @param userDetails the authenticated user, or null if anonymous
     * @param status      optional play-status filter
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @param sort        the sort criteria (e.g., "createdAt,desc")
     * @return paginated list of user-game DTOs
     */
    @GetMapping("/{username}/library")
    public ResponseEntity<PagedResponseDto<UserGameResponseDto>> getUserLibrary(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PlayStatus status,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/v1/users/{}/library - status: {}, page: {}, size: {}", username, status, page, size);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<UserGameResponseDto> library = profileService.getUserLibrary(username, viewerEmail, status, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(library));
    }

    /**
     * Returns a paginated list of games in the given user's backlog.
     *
     * @param username    the user's display name (pseudo)
     * @param userDetails the authenticated user, or null if anonymous
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @param sort        the sort criteria (e.g., "createdAt,desc" or "priority,desc")
     * @return paginated list of backlog DTOs
     */
    @GetMapping("/{username}/backlog")
    public ResponseEntity<PagedResponseDto<BacklogResponseDto>> getUserBacklog(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/v1/users/{}/backlog - page: {}, size: {}", username, page, size);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<BacklogResponseDto> backlog = profileService.getUserBacklog(username, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(backlog));
    }

    /**
     * Returns a paginated list of the given user's play log (journal) entries.
     *
     * @param username    the user's display name (pseudo)
     * @param userDetails the authenticated user, or null if anonymous
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20, max 100)
     * @param sort        the sort criteria (e.g., "updatedAt,desc")
     * @return paginated list of play log DTOs
     */
    @GetMapping("/{username}/plays")
    public ResponseEntity<PagedResponseDto<GamePlayLogResponseDto>> getUserPlayLog(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {

        log.info("GET /api/v1/users/{}/plays - page: {}, size: {}", username, page, size);

        String viewerEmail = userDetails != null ? userDetails.getUsername() : null;
        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<GamePlayLogResponseDto> plays = profileService.getUserPlayLog(username, viewerEmail, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(plays));
    }

    /**
     * Returns a paginated list of public game lists for the given user.
     *
     * @param username the user's display name (pseudo)
     * @param page     the page number (0-based, default 0)
     * @param size     the page size (default 20, max 100)
     * @param sort     the sort criteria (e.g., "createdAt,desc")
     * @return paginated list of game list card DTOs
     */
    @GetMapping("/{username}/lists")
    public ResponseEntity<PagedResponseDto<GameListCardDto>> getUserLists(
            @PathVariable String username,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("GET /api/v1/users/{}/lists - page: {}, size: {}", username, page, size);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<GameListCardDto> lists = profileService.getUserLists(username, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(lists));
    }

    /**
     * Compares the authenticated viewer's profile with the given user's profile.
     * Requires authentication (enforced by the security configuration).
     *
     * @param username    the compared user's display name (pseudo)
     * @param userDetails the authenticated viewer
     * @param page        the page number for common games (0-based, default 0)
     * @param size        the page size for common games (default 20, max 100)
     * @return the profile comparison DTO
     */
    @GetMapping("/{username}/compare")
    public ResponseEntity<ProfileComparisonDto> compareProfiles(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {

        log.info("GET /api/v1/users/{}/compare - viewer: {}", username, userDetails.getUsername());

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize);
        ProfileComparisonDto comparison = profileComparisonService.compare(
                userDetails.getUsername(), username, pageable);

        return ResponseEntity.ok(comparison);
    }

    /**
     * Toggles the follow status between the authenticated user and the target user.
     * If the user is already following the target, they will unfollow. Otherwise, they will follow.
     *
     * @param userDetails the authenticated user principal
     * @param userId      the target user's ID
     * @return the new follow status
     */
    @PostMapping("/{userId}/follow")
    public ResponseEntity<FollowResponseDto> toggleFollow(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId) {

        log.info("POST /api/v1/users/{}/follow - user: {}", userId, userDetails.getUsername());

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
            @RequestParam(defaultValue = DEFAULT_FOLLOW_SORT) String sort) {

        log.info("GET /api/v1/users/{}/followers - page: {}, size: {}, sort: {}", userId, page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createFollowPageable(validatedPage, validatedSize, sort);
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
            @RequestParam(defaultValue = DEFAULT_FOLLOW_SORT) String sort) {

        log.info("GET /api/v1/users/{}/following - page: {}, size: {}, sort: {}", userId, page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createFollowPageable(validatedPage, validatedSize, sort);
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

        log.info("DELETE /api/v1/users/me/followers/{} - user: {}", followerId, userDetails.getUsername());

        followService.removeFollower(userDetails.getUsername(), followerId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a Pageable from a "field,direction" sort string (e.g. "createdAt,desc"),
     * using the field name as-is.
     */
    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1
                && sortParts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }

    /**
     * Creates a Pageable for follower/following listings, mapping the API sort
     * field to the underlying entity field via {@link #mapSortField(String)}.
     */
    private Pageable createFollowPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1
                && sortParts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, mapSortField(sortField)));
    }

    /**
     * Maps API sort field names to entity field names for follow listings.
     */
    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "pseudo", "username", "name" -> "pseudo";
            case "createdat", "created_at" -> "createdAt";
            default -> "pseudo";
        };
    }
}
