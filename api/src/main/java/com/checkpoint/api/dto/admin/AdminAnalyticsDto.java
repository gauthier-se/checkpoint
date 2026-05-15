package com.checkpoint.api.dto.admin;

import java.util.List;
import java.util.UUID;

/**
 * DTO for the admin analytics dashboard.
 * Aggregates platform-wide KPIs and top-5 lists in a single payload.
 *
 * @param totalUsers       total number of registered users
 * @param activeUsers      number of users that are not banned
 * @param totalGames       number of standalone games (DLCs excluded)
 * @param totalReviews     total number of reviews
 * @param openReports      number of reports awaiting moderation (dismissed reports are deleted)
 * @param topReviewedGames the five games with the most reviews
 * @param topReviewers     the five users with the most reviews
 */
public record AdminAnalyticsDto(
        long totalUsers,
        long activeUsers,
        long totalGames,
        long totalReviews,
        long openReports,
        List<TopGame> topReviewedGames,
        List<TopReviewer> topReviewers
) {

    /**
     * A game ranked among the most reviewed.
     *
     * @param id          the game's UUID
     * @param title       the game's title
     * @param reviewCount the number of reviews targeting this game
     */
    public record TopGame(UUID id, String title, long reviewCount) {}

    /**
     * A user ranked among the most active reviewers.
     *
     * @param id          the user's UUID
     * @param username    the user's pseudo
     * @param reviewCount the number of reviews written by this user
     */
    public record TopReviewer(UUID id, String username, long reviewCount) {}
}
