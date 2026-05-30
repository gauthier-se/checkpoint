package com.checkpoint.api.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.social.FeedItemDto;
import com.checkpoint.api.enums.FeedItemType;
import com.checkpoint.api.services.FeedService;

/**
 * Controller for the authenticated user's activity feed and friends trending data.
 * All endpoints require authentication.
 */
@Tag(name = "Notifications and Feed", description = "Current user activity feed")
@RestController
@RequestMapping("/api/me")
public class FeedController {

    private static final Logger log = LoggerFactory.getLogger(FeedController.class);

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
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

        log.info("GET /api/me/feed - user: {}, page: {}, size: {}, type: {}",
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

        log.info("GET /api/me/friends/trending-games - user: {}, size: {}", userDetails.getUsername(), size);

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

        log.info("GET /api/me/friends/popular-games - user: {}, page: {}, size: {}",
                userDetails.getUsername(), page, size);

        PagedResponseDto<GameCardDto> popular = feedService.getFriendsPopularGames(
                userDetails.getUsername(), page, size);
        return ResponseEntity.ok(popular);
    }
}
