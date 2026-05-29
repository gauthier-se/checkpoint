package com.checkpoint.api.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.ReviewCardDto;
import com.checkpoint.api.dto.catalog.ReviewRequestDto;
import com.checkpoint.api.dto.catalog.ReviewResponseDto;

/**
 * Service for managing game reviews.
 * Reviews are tied to play log entries, allowing multiple reviews per game (one per playthrough).
 */
public interface ReviewService {

    /**
     * Retrieves a paginated list of all reviews for a specific video game.
     * Includes reviews from all users and all their play logs.
     * When a viewer email is provided, the response includes whether the viewer has liked each review.
     *
     * @param videoGameId the video game ID
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @param pageable    pagination and sorting details
     * @return a page of reviews
     */
    Page<ReviewResponseDto> getGameReviews(UUID videoGameId, String viewerEmail, Pageable pageable);

    /**
     * Creates a review attached to a specific play log entry.
     *
     * @param userEmail the authenticated user's email
     * @param playId    the play log ID
     * @param request   the review request containing content and spoiler flag
     * @return the created review
     * @throws com.checkpoint.api.exceptions.PlayLogNotFoundException      if the play log does not exist or does not belong to the user
     * @throws com.checkpoint.api.exceptions.ReviewAlreadyExistsException  if the play log already has a review
     */
    ReviewResponseDto createPlayLogReview(String userEmail, UUID playId, ReviewRequestDto request);

    /**
     * Updates the review of a specific play log entry.
     *
     * @param userEmail the authenticated user's email
     * @param playId    the play log ID
     * @param request   the review request containing updated content and spoiler flag
     * @return the updated review
     * @throws com.checkpoint.api.exceptions.PlayLogNotFoundException  if the play log does not exist or does not belong to the user
     * @throws com.checkpoint.api.exceptions.ReviewNotFoundException   if no review exists for the play log
     */
    ReviewResponseDto updatePlayLogReview(String userEmail, UUID playId, ReviewRequestDto request);

    /**
     * Deletes the review of a specific play log entry.
     *
     * @param userEmail the authenticated user's email
     * @param playId    the play log ID
     * @throws com.checkpoint.api.exceptions.PlayLogNotFoundException  if the play log does not exist or does not belong to the user
     * @throws com.checkpoint.api.exceptions.ReviewNotFoundException   if no review exists for the play log
     */
    void deletePlayLogReview(String userEmail, UUID playId);

    /**
     * Retrieves the review of a specific play log entry.
     *
     * @param userEmail the authenticated user's email
     * @param playId    the play log ID
     * @return the review
     * @throws com.checkpoint.api.exceptions.PlayLogNotFoundException  if the play log does not exist or does not belong to the user
     * @throws com.checkpoint.api.exceptions.ReviewNotFoundException   if no review exists for the play log
     */
    ReviewResponseDto getPlayLogReview(String userEmail, UUID playId);

    /**
     * Returns the top reviews ranked by a "hot" score combining likes count and recency.
     * When a viewer email is provided, the response includes whether the viewer has liked each review.
     *
     * @param size        the maximum number of reviews to return
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @return the popular reviews bundled with their game information
     */
    List<ReviewCardDto> getPopularReviews(int size, String viewerEmail);

    /**
     * Returns the most recently created reviews across all games and users.
     * When a viewer email is provided, the response includes whether the viewer has liked each review.
     *
     * @param size        the maximum number of reviews to return
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @return the recent reviews bundled with their game information
     */
    List<ReviewCardDto> getRecentReviews(int size, String viewerEmail);

    /**
     * Returns the top reviews for a single game ranked by the same "hot" score
     * as {@link #getPopularReviews(int, String)}.
     *
     * @param videoGameId the video game ID
     * @param size        the maximum number of reviews to return
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @return the popular reviews for the game (never null; may be empty)
     */
    List<ReviewCardDto> getPopularGameReviews(UUID videoGameId, int size, String viewerEmail);

    /**
     * Returns reviews authored by the viewer's followings for a specific game,
     * ordered by creation date (descending). When the viewer is anonymous or
     * follows nobody, returns an empty page.
     *
     * @param videoGameId the video game ID
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @param pageable    pagination parameters; sort is ignored
     * @return a page of friend reviews
     */
    Page<ReviewResponseDto> getFriendReviewsForGame(UUID videoGameId, String viewerEmail, Pageable pageable);
}
