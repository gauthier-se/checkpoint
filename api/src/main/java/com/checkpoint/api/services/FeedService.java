package com.checkpoint.api.services;

import java.util.List;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.social.FeedItemDto;

/**
 * Service for retrieving activity feed and friends-related trending data.
 */
public interface FeedService {

    /**
     * Returns a paginated activity feed from users the authenticated user follows.
     * Aggregates play sessions, ratings, reviews, and list creations from the last 30 days.
     *
     * @param userEmail the authenticated user's email
     * @param page      the page number (0-based)
     * @param size      the page size
     * @return a paginated response of feed items
     */
    PagedResponseDto<FeedItemDto> getFeed(String userEmail, int page, int size);

    /**
     * Returns trending games among the authenticated user's followed users.
     * Uses the same weighted scoring as global trending but filtered to the follow graph.
     *
     * @param userEmail the authenticated user's email
     * @param size      the maximum number of games to return
     * @return a list of trending game cards among friends
     */
    List<GameCardDto> getFriendsTrendingGames(String userEmail, int size);

    /**
     * Paginated variant of {@link #getFriendsTrendingGames} for the dedicated
     * "Popular with friends" page. Only includes games with at least one
     * interaction from the follow graph within the trending window.
     *
     * @param userEmail the authenticated user's email
     * @param page      the page number (0-based)
     * @param size      the page size
     * @return a paginated response of trending game cards among friends
     */
    PagedResponseDto<GameCardDto> getFriendsPopularGames(String userEmail, int page, int size);
}
