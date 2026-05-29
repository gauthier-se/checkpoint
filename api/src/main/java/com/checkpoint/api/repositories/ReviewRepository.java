package com.checkpoint.api.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.checkpoint.api.entities.Review;

/**
 * Repository for {@link Review} entity.
 */
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Checks whether any review exists for a given user and video game.
     * A user may have multiple reviews for the same game (one per playthrough).
     *
     * @param pseudo      the user's pseudo
     * @param videoGameId the video game ID
     * @return true if at least one review exists
     */
    boolean existsByUserPseudoAndVideoGameId(String pseudo, UUID videoGameId);

    /**
     * Finds all reviews for a specific video game.
     *
     * @param videoGameId the video game ID
     * @param pageable pagination and sorting details
     * @return a page of reviews
     */
    Page<Review> findByVideoGameId(UUID videoGameId, Pageable pageable);

    /**
     * Finds a review by its associated play log ID.
     *
     * @param userGamePlayId the play log ID
     * @return an optional containing the review if found
     */
    Optional<Review> findByUserGamePlayId(UUID userGamePlayId);

    /**
     * Checks whether a review exists for a specific play log.
     *
     * @param userGamePlayId the play log ID
     * @return true if a review exists for the play log
     */
    boolean existsByUserGamePlayId(UUID userGamePlayId);

    /**
     * Finds all reviews written by a user with the given pseudo.
     *
     * @param pseudo   the user's pseudo
     * @param pageable pagination and sorting details
     * @return a page of reviews
     */
    Page<Review> findByUserPseudo(String pseudo, Pageable pageable);

    /**
     * Counts the number of reviews written by a user with the given pseudo.
     *
     * @param pseudo the user's pseudo
     * @return the review count
     */
    long countByUserPseudo(String pseudo);

    /**
     * Counts the number of reviews written by a user with the given ID.
     *
     * @param userId the user's ID
     * @return the review count
     */
    long countByUserId(UUID userId);

    /**
     * Counts how many reviews reference a given video game.
     * Used by the admin delete-game integrity check.
     */
    long countByVideoGameId(UUID videoGameId);

    /**
     * Finds all reviews that have at least one report.
     *
     * @param pageable pagination and sorting details
     * @return a page of reported reviews
     */
    Page<Review> findByReportsIsNotEmpty(Pageable pageable);

    /**
     * Finds video games ranked by review count (descending).
     * Returns each game's id and title alongside the review count.
     *
     * @param pageable pagination parameters (used to cap the result size)
     * @return a page of Object[] where [0] = game id (UUID), [1] = title (String), [2] = review count (Long)
     */
    @Query("SELECT r.videoGame.id, r.videoGame.title, COUNT(r) AS rc "
            + "FROM Review r "
            + "GROUP BY r.videoGame.id, r.videoGame.title "
            + "ORDER BY rc DESC")
    Page<Object[]> findTopReviewedGames(Pageable pageable);

    /**
     * Returns the user's three most recent reviews, ordered by creation time (descending).
     * Used by the badge system to detect a streak of three consecutive 5-star reviews.
     *
     * @param userId the user's ID
     * @return up to three most recent reviews
     */
    List<Review> findTop3ByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Counts the user's reviews that are tied to a play log with a harsh score
     * ({@code userGamePlay.score <= 2}, i.e. 0.5 or 1 star on the 5-star display).
     * Used by the badge system to evaluate the {@code BRUTAL_CRITIC} threshold.
     *
     * @param userId the user's ID
     * @return the number of "1-star" reviews
     */
    @Query("""
            SELECT COUNT(r) FROM Review r
            WHERE r.user.id = :userId AND r.userGamePlay.score <= 2
            """)
    long countOneStarReviewsByUserId(@Param("userId") UUID userId);

    /**
     * Checks whether the user has at least one review with content of length 1000+.
     * Used by the badge system to evaluate the {@code WORDSMITH} threshold.
     *
     * @param userId the user's ID
     * @return true if any review qualifies
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Review r
            WHERE r.user.id = :userId AND LENGTH(r.content) >= 1000
            """)
    boolean existsLongReviewByUserId(@Param("userId") UUID userId);

    /**
     * Finds the top reviews ranked by a "hot" score combining likes count and recency.
     * Uses a Reddit-style gravity-1.5 decay over days since creation:
     * {@code likes_count / POWER(EXTRACT(EPOCH FROM (NOW() - created_at)) / 86400.0 + 2, 1.5)}.
     *
     * @param limit the maximum number of reviews to return
     * @return the top reviews ordered by hot score (descending)
     */
    @Query(value = """
            SELECT r.*
            FROM reviews r
            LEFT JOIN likes l ON l.review_id = r.id
            GROUP BY r.id
            ORDER BY (COUNT(l.id)::float
                      / POWER(EXTRACT(EPOCH FROM (NOW() - r.created_at)) / 86400.0 + 2, 1.5)) DESC,
                     r.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Review> findPopularReviews(@Param("limit") int limit);

    /**
     * Finds the most recently created reviews across all games and users.
     *
     * @param pageable pagination parameters (used to cap the result size; sort is ignored)
     * @return a page of reviews ordered by creation time (descending)
     */
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Finds the top reviews for a single game ranked by the same "hot" score as
     * {@link #findPopularReviews(int)}.
     *
     * @param videoGameId the video game ID
     * @param limit       the maximum number of reviews to return
     * @return the top reviews for the game ordered by hot score (descending)
     */
    @Query(value = """
            SELECT r.*
            FROM reviews r
            LEFT JOIN likes l ON l.review_id = r.id
            WHERE r.video_game_id = :videoGameId
            GROUP BY r.id
            ORDER BY (COUNT(l.id)::float
                      / POWER(EXTRACT(EPOCH FROM (NOW() - r.created_at)) / 86400.0 + 2, 1.5)) DESC,
                     r.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Review> findPopularReviewsForGame(@Param("videoGameId") UUID videoGameId,
                                            @Param("limit") int limit);

    /**
     * Finds reviews authored by any of the given users for a specific game,
     * ordered by creation date (descending). Used to feed the "Reviews from friends"
     * panel on the game detail page.
     *
     * @param videoGameId the video game ID
     * @param userIds     the candidate author IDs (typically the viewer's followings)
     * @param pageable    pagination parameters; sort is ignored
     * @return a page of friend reviews for the game
     */
    @Query("""
            SELECT r FROM Review r
            WHERE r.videoGame.id = :videoGameId
              AND r.user.id IN :userIds
            ORDER BY r.createdAt DESC
            """)
    Page<Review> findByVideoGameIdAndUserIdInOrderByCreatedAtDesc(
            @Param("videoGameId") UUID videoGameId,
            @Param("userIds") Collection<UUID> userIds,
            Pageable pageable);

}
