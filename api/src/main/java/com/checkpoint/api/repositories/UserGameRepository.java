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

import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.enums.PlayStatus;

/**
 * Repository for {@link UserGame} entities.
 */
@Repository
public interface UserGameRepository extends JpaRepository<UserGame, UUID> {

    /**
     * Finds a user-game association by user ID and video game ID.
     */
    Optional<UserGame> findByUserIdAndVideoGameId(UUID userId, UUID videoGameId);

    /**
     * Returns the games in a user's library (paginated), optionally filtered by status,
     * with the video game eagerly fetched and the user's rating left-joined.
     * Each row is a tuple {@code [UserGame, Integer rateScore]} where the rate score is
     * {@code null} when the user hasn't rated the game. Sorting is delegated to the
     * supplied {@link Pageable} (entity fields only — to sort by rating, use
     * {@link #findLibraryProjectionSortedByRating}).
     */
    @Query("""
            SELECT ug, r.score FROM UserGame ug
            JOIN FETCH ug.videoGame
            LEFT JOIN Rate r ON r.user.id = ug.user.id AND r.videoGame.id = ug.videoGame.id
            WHERE ug.user.id = :userId
              AND (:status IS NULL OR ug.status = :status)
            """)
    Page<Object[]> findLibraryProjection(@Param("userId") UUID userId,
                                         @Param("status") PlayStatus status,
                                         Pageable pageable);

    /**
     * Same projection as {@link #findLibraryProjection} but forces the row order to
     * the user's rating descending (unrated games last). The supplied {@link Pageable}'s
     * sort is ignored — only page number and size are honoured.
     */
    @Query(value = """
            SELECT ug, r.score FROM UserGame ug
            JOIN FETCH ug.videoGame
            LEFT JOIN Rate r ON r.user.id = ug.user.id AND r.videoGame.id = ug.videoGame.id
            WHERE ug.user.id = :userId
              AND (:status IS NULL OR ug.status = :status)
            ORDER BY r.score DESC NULLS LAST, ug.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(ug) FROM UserGame ug
            WHERE ug.user.id = :userId
              AND (:status IS NULL OR ug.status = :status)
            """)
    Page<Object[]> findLibraryProjectionSortedByRating(@Param("userId") UUID userId,
                                                       @Param("status") PlayStatus status,
                                                       Pageable pageable);

    /**
     * Checks if a user already has a specific game in their library.
     */
    boolean existsByUserIdAndVideoGameId(UUID userId, UUID videoGameId);

    /**
     * Returns the IDs of every video game from {@code videoGameIds} that is already
     * in the given user's library. Used by the Steam library sync to skip duplicates
     * in a single round-trip.
     */
    @Query("""
            SELECT ug.videoGame.id FROM UserGame ug
            WHERE ug.user.id = :userId AND ug.videoGame.id IN :videoGameIds
            """)
    List<UUID> findExistingVideoGameIds(@Param("userId") UUID userId,
                                        @Param("videoGameIds") Collection<UUID> videoGameIds);

    /**
     * Deletes a user-game association by user ID and video game ID.
     */
    void deleteByUserIdAndVideoGameId(UUID userId, UUID videoGameId);

    /**
     * Counts the number of games in a user's library that are in the given status.
     * Used by the badge system to evaluate completion thresholds.
     */
    long countByUserIdAndStatus(UUID userId, PlayStatus status);

    /**
     * Counts the total number of games in a user's library, regardless of status.
     * Used by the badge system to evaluate library-size thresholds.
     */
    long countByUserId(UUID userId);

    /**
     * Counts how many library entries reference a given video game.
     * Used by the admin delete-game integrity check.
     */
    long countByVideoGameId(UUID videoGameId);

    /**
     * Counts the number of COMPLETED library entries whose video game has at least
     * one genre matching the given name (case-insensitive). Used by the badge system
     * to evaluate genre-completion thresholds (e.g. "finish 10 RPGs").
     */
    @Query("""
            SELECT COUNT(ug) FROM UserGame ug JOIN ug.videoGame.genres g
            WHERE ug.user.id = :userId
              AND ug.status = com.checkpoint.api.enums.PlayStatus.COMPLETED
              AND LOWER(g.name) = LOWER(:genreName)
            """)
    long countCompletedByUserIdAndGenreName(@Param("userId") UUID userId,
                                            @Param("genreName") String genreName);

    /**
     * Returns every library entry for the given user. Used by the recommendation service
     * to weight games by status (COMPLETED, PLAYING…) when building the affinity profile.
     */
    List<UserGame> findAllByUserId(UUID userId);

    /**
     * Returns the IDs of video games that appear in both users' libraries.
     * Used by the profile comparison feature to find games in common.
     */
    @Query("""
            SELECT ug1.videoGame.id FROM UserGame ug1, UserGame ug2
            WHERE ug1.videoGame.id = ug2.videoGame.id
              AND ug1.user.id = :userId1
              AND ug2.user.id = :userId2
            """)
    List<UUID> findCommonVideoGameIds(@Param("userId1") UUID userId1,
                                      @Param("userId2") UUID userId2);

    /**
     * Returns the given user's library entries for the supplied video game IDs, with the
     * video game eagerly fetched. Used by the profile comparison feature to resolve both
     * users' statuses (and game metadata) for the common games in a single round-trip.
     */
    @Query("""
            SELECT ug FROM UserGame ug
            JOIN FETCH ug.videoGame
            WHERE ug.user.id = :userId AND ug.videoGame.id IN :gameIds
            """)
    List<UserGame> findByUserIdAndVideoGameIdIn(@Param("userId") UUID userId,
                                                @Param("gameIds") List<UUID> gameIds);

    /**
     * Counts the union of two users' libraries (distinct video games owned by either user).
     * Used as the Jaccard denominator when computing the profile comparison affinity score.
     */
    @Query("""
            SELECT COUNT(DISTINCT ug.videoGame.id) FROM UserGame ug
            WHERE ug.user.id IN (:userId1, :userId2)
            """)
    long countDistinctGamesByUserIds(@Param("userId1") UUID userId1,
                                     @Param("userId2") UUID userId2);

    /**
     * Returns true if the user has at least one COMPLETED library entry whose
     * video game's release date is on or before the given cutoff. Powers the
     * {@code TIME_TRAVELER} easter-egg badge (finish a game ≥ 30 years old).
     */
    @Query("""
            SELECT COUNT(ug) > 0 FROM UserGame ug
            WHERE ug.user.id = :userId
              AND ug.status = com.checkpoint.api.enums.PlayStatus.COMPLETED
              AND ug.videoGame.releaseDate IS NOT NULL
              AND ug.videoGame.releaseDate <= :cutoff
            """)
    boolean existsCompletedGameOlderThan(@Param("userId") UUID userId,
                                          @Param("cutoff") java.time.LocalDate cutoff);
}
