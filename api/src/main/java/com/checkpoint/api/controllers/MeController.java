package com.checkpoint.api.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.export.UserDataExportDto;
import com.checkpoint.api.dto.profile.ProfileUpdatedDto;
import com.checkpoint.api.dto.profile.UpdateProfileDto;
import com.checkpoint.api.dto.social.FeedItemDto;
import com.checkpoint.api.enums.FeedItemType;
import com.checkpoint.api.services.AccountService;
import com.checkpoint.api.services.AuthService;
import com.checkpoint.api.services.DataExportService;
import com.checkpoint.api.services.FeedService;
import com.checkpoint.api.services.ProfileService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * REST controller for the authenticated user acting on their own resources
 * ({@code /me}): account lifecycle and data export, profile and picture
 * editing, and the personal activity feed.
 */
@Tag(name = "Account and Profile", description = "Current user account, profile, and activity feed")
@RestController
@RequestMapping("/me")
public class MeController {

    private static final Logger log = LoggerFactory.getLogger(MeController.class);

    private final AccountService accountService;
    private final AuthService authService;
    private final DataExportService dataExportService;
    private final ProfileService profileService;
    private final FeedService feedService;

    public MeController(AccountService accountService,
                        AuthService authService,
                        DataExportService dataExportService,
                        ProfileService profileService,
                        FeedService feedService) {
        this.accountService = accountService;
        this.authService = authService;
        this.dataExportService = dataExportService;
        this.profileService = profileService;
        this.feedService = feedService;
    }

    /**
     * Permanently deletes the authenticated user's account and every piece of
     * personal data associated with it (GDPR Article 17 — right to erasure).
     *
     * <p>The session is invalidated: the refresh token row is destroyed by the
     * account-erasure transaction and both the {@code checkpoint_token} and
     * {@code checkpoint_refresh} cookies are expired on the response so the
     * next request from this browser will be unauthenticated.</p>
     *
     * @param userDetails   the authenticated user principal
     * @param refreshCookie the {@code checkpoint_refresh} cookie value, used
     *                      only to expire the cookie on the response
     * @param response      the HTTP response to write the expired cookies on
     * @return 204 No Content
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @CookieValue(value = "checkpoint_refresh", required = false) String refreshCookie,
            HttpServletResponse response) {

        log.info("DELETE /api/v1/me - user: {}", userDetails.getUsername());

        accountService.deleteCurrentUser(userDetails.getUsername());
        authService.clearAuthCookie(refreshCookie, response);

        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the authenticated user's personal data in a single
     * machine-readable JSON file (GDPR Article 20 — right to data portability).
     *
     * <p>The response body is the export payload itself. The
     * {@code Content-Disposition} header instructs the browser to save it as
     * {@code checkpoint-export-YYYY-MM-DD.json} rather than render it inline.</p>
     *
     * @param userDetails the authenticated user principal
     * @return 200 OK with the export payload, served as a JSON download
     */
    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDataExportDto> exportData(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/me/export - user: {}", userDetails.getUsername());

        UserDataExportDto export = dataExportService.exportForUser(userDetails.getUsername());
        String filename = "checkpoint-export-" + LocalDate.now() + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(export);
    }

    /**
     * Updates the authenticated user's profile information (pseudo, bio, privacy).
     *
     * @param userDetails the authenticated user
     * @param request     the profile update data
     * @return the updated profile DTO
     */
    @PutMapping("/profile")
    public ResponseEntity<ProfileUpdatedDto> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileDto request) {

        log.info("PUT /api/v1/me/profile - user: {}", userDetails.getUsername());

        ProfileUpdatedDto updated = profileService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Uploads or replaces the authenticated user's profile picture.
     *
     * @param userDetails the authenticated user
     * @param file        the uploaded image file
     * @return a map containing the new picture URL
     */
    @PostMapping("/picture")
    public ResponseEntity<Map<String, String>> updatePicture(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

        log.info("POST /api/v1/me/picture - user: {}", userDetails.getUsername());

        String pictureUrl = profileService.updatePicture(userDetails.getUsername(), file);
        return ResponseEntity.ok(Map.of("picture", pictureUrl));
    }

    /**
     * Removes the authenticated user's profile picture.
     *
     * @param userDetails the authenticated user
     * @return no content
     */
    @DeleteMapping("/picture")
    public ResponseEntity<Void> deletePicture(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("DELETE /api/v1/me/picture - user: {}", userDetails.getUsername());

        profileService.deletePicture(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns a paginated activity feed from users the authenticated user follows.
     * Aggregates play sessions, ratings, reviews, and list creations.
     *
     * @param userDetails the authenticated user
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 20)
     * @param type        optional activity type filter (null = all types)
     * @return a paginated response of feed items
     */
    @GetMapping("/feed")
    public ResponseEntity<PagedResponseDto<FeedItemDto>> getFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) FeedItemType type) {

        log.info("GET /api/v1/me/feed - user: {}, page: {}, size: {}, type: {}",
                userDetails.getUsername(), page, size, type);

        PagedResponseDto<FeedItemDto> feed = feedService.getFeed(userDetails.getUsername(), page, size, type);
        return ResponseEntity.ok(feed);
    }

    /**
     * Returns trending games among the authenticated user's followed users.
     * Uses the same weighted scoring as global trending but filtered to the follow graph.
     *
     * @param userDetails the authenticated user
     * @param size        the number of trending games to return (default 7)
     * @return a list of trending game cards among friends
     */
    @GetMapping("/friends/trending-games")
    public ResponseEntity<List<GameCardDto>> getFriendsTrendingGames(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "7") int size) {

        log.info("GET /api/v1/me/friends/trending-games - user: {}, size: {}", userDetails.getUsername(), size);

        List<GameCardDto> trending = feedService.getFriendsTrendingGames(userDetails.getUsername(), size);
        return ResponseEntity.ok(trending);
    }

    /**
     * Returns a paginated list of games popular among the authenticated user's followed users.
     * Only includes games that have received at least one interaction from the follow graph
     * within the trending window.
     *
     * @param userDetails the authenticated user
     * @param page        the page number (0-based, default 0)
     * @param size        the page size (default 32, max 50)
     * @return a paginated response of trending game cards among friends
     */
    @GetMapping("/friends/popular-games")
    public ResponseEntity<PagedResponseDto<GameCardDto>> getFriendsPopularGames(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "32") int size) {

        log.info("GET /api/v1/me/friends/popular-games - user: {}, page: {}, size: {}",
                userDetails.getUsername(), page, size);

        PagedResponseDto<GameCardDto> popular = feedService.getFriendsPopularGames(
                userDetails.getUsername(), page, size);
        return ResponseEntity.ok(popular);
    }
}
