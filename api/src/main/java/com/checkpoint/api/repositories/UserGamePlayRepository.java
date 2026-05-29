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
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.UserGamePlay;

/**
 * Repository for {@link UserGamePlay} entities.
 */
@Repository
public interface UserGamePlayRepository extends JpaRepository<UserGamePlay, UUID> {

    /**
     * Returns all play logs for a user (paginated), with video game and platform eagerly fetched.
     */
    @Query("""
            SELECT p FROM UserGamePlay p
            JOIN FETCH p.videoGame
            JOIN FETCH p.platform
            WHERE p.user.id = :userId
            """)
    Page<UserGamePlay> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Returns all play entries for a specific user and game.
     */
    @Query("""
            SELECT p FROM UserGamePlay p
            JOIN FETCH p.videoGame
            JOIN FETCH p.platform
            WHERE p.user.id = :userId AND p.videoGame.id = :videoGameId
            """)
    List<UserGamePlay> findByUserIdAndVideoGameId(@Param("userId") UUID userId, @Param("videoGameId") UUID videoGameId);

    /**
     * Counts the number of play entries for a specific user and game.
     */
    long countByUserIdAndVideoGameId(UUID userId, UUID videoGameId);

    /**
     * Returns a play log with its video game, platform, author, tags and review
     * eagerly fetched. Used by the public play-log detail endpoint to avoid N+1.
     *
     * @param id the play log ID
     * @return the play log if it exists
     */
    @Query("""
            SELECT p FROM UserGamePlay p
            JOIN FETCH p.videoGame
            JOIN FETCH p.platform
            JOIN FETCH p.user
            LEFT JOIN FETCH p.review
            LEFT JOIN FETCH p.tags
            WHERE p.id = :id
            """)
    Optional<UserGamePlay> findByIdWithRelations(@Param("id") UUID id);

    /**
     * Returns the most recent play logs for a user, ordered by createdAt descending.
     * The {@code videoGame} and {@code review} associations are eagerly fetched
     * so the caller can populate {@code RecentPlayDto} without N+1 queries.
     * Use {@code PageRequest.of(0, 5)} to limit to 5.
     */
    @Query("""
            SELECT p FROM UserGamePlay p
            JOIN FETCH p.videoGame
            LEFT JOIN FETCH p.review
            WHERE p.user.id = :userId
            ORDER BY p.createdAt DESC
            """)
    List<UserGamePlay> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Finds the most recent play log with a non-null score for a specific user and game.
     * Used to recalculate the global rating after a scored log is updated or deleted.
     *
     * @param userId      the user ID
     * @param videoGameId the video game ID
     * @return the most recent scored play log, if any
     */
    @Query("""
            SELECT p FROM UserGamePlay p
            WHERE p.user.id = :userId
              AND p.videoGame.id = :videoGameId
              AND p.score IS NOT NULL
            ORDER BY p.createdAt DESC
            LIMIT 1
            """)
    Optional<UserGamePlay> findMostRecentScoredPlay(@Param("userId") UUID userId,
                                                     @Param("videoGameId") UUID videoGameId);

    /**
     * Counts the total number of play logs for a user. Used by the badge system
     * to evaluate the play-count threshold (e.g. {@code CENTURION}).
     */
    long countByUserId(UUID userId);

    /**
     * Counts how many play logs reference a given video game.
     * Used by the admin delete-game integrity check.
     */
    long countByVideoGameId(UUID videoGameId);

    /**
     * Counts the number of distinct platforms across all of the user's play logs.
     * Used by the badge system to evaluate platform-diversity badges
     * (e.g. {@code MULTIPLATFORM_NOMAD}).
     */
    @Query("SELECT COUNT(DISTINCT p.platform.id) FROM UserGamePlay p WHERE p.user.id = :userId")
    long countDistinctPlatformsByUserId(@Param("userId") UUID userId);

    /**
     * Returns the most recent play log (by {@code createdAt}) for each of the given
     * users on a specific game, eagerly fetching the linked user, review reference,
     * and platform so the consumer can render friend-activity entries without N+1.
     *
     * @param videoGameId the video game ID
     * @param userIds     the candidate user IDs (typically the viewer's followings)
     * @return one play log per matching user, or an empty list if {@code userIds} is empty
     */
    @Query("""
            SELECT p FROM UserGamePlay p
            JOIN FETCH p.user
            LEFT JOIN FETCH p.review
            WHERE p.videoGame.id = :videoGameId
              AND p.user.id IN :userIds
              AND p.createdAt = (
                  SELECT MAX(p2.createdAt) FROM UserGamePlay p2
                  WHERE p2.videoGame.id = :videoGameId
                    AND p2.user.id = p.user.id
              )
            """)
    List<UserGamePlay> findLatestPerUserForGame(@Param("videoGameId") UUID videoGameId,
                                                 @Param("userIds") Collection<UUID> userIds);

    /**
     * Returns true if the user has at least one play log whose {@code createdAt}
     * hour is 02, 03 or 04 (server time). Powers the {@code NIGHT_OWL} easter-egg
     * badge ("log a play between 02:00 and 05:00").
     *
     * <p>Server-time is used as a proxy for the user's local time since the
     * platform does not currently track per-user timezone. The badge is meant
     * to be fun rather than precise.</p>
     */
    @Query(value = """
            SELECT COUNT(*) > 0 FROM user_game_plays
            WHERE user_id = :userId
              AND EXTRACT(HOUR FROM created_at) IN (2, 3, 4)
            """, nativeQuery = true)
    boolean existsNightOwlPlayByUserId(@Param("userId") UUID userId);
}
