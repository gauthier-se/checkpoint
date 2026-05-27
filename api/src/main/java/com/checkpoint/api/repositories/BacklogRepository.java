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

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.entities.Backlog;

/**
 * Repository for {@link Backlog} entities.
 */
@Repository
public interface BacklogRepository extends JpaRepository<Backlog, UUID> {

    /**
     * Checks if a user already has a specific game in their backlog.
     */
    boolean existsByUserIdAndVideoGameId(UUID userId, UUID videoGameId);

    /**
     * Finds a backlog entry by user ID and video game ID.
     */
    Optional<Backlog> findByUserIdAndVideoGameId(UUID userId, UUID videoGameId);

    /**
     * Returns the IDs of every video game from {@code videoGameIds} that is already
     * in the given user's backlog. Used by the Steam library sync to skip duplicates
     * in a single round-trip.
     */
    @Query("""
            SELECT b.videoGame.id FROM Backlog b
            WHERE b.user.id = :userId AND b.videoGame.id IN :videoGameIds
            """)
    List<UUID> findExistingVideoGameIds(@Param("userId") UUID userId,
                                        @Param("videoGameIds") Collection<UUID> videoGameIds);

    /**
     * Returns all games in a user's backlog (paginated), with video game eagerly fetched.
     */
    @Query("""
            SELECT b FROM Backlog b
            JOIN FETCH b.videoGame
            WHERE b.user.id = :userId
            """)
    Page<Backlog> findByUserIdWithVideoGame(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Returns the user's backlog ordered by priority descending: HIGH → MEDIUM → LOW → NULL.
     * Matches {@code ?sort=priority,desc}. Pageable's own sort is ignored.
     */
    @Query("""
            SELECT b FROM Backlog b
            JOIN FETCH b.videoGame
            WHERE b.user.id = :userId
            ORDER BY CASE b.priority
                       WHEN com.checkpoint.api.enums.Priority.HIGH   THEN 3
                       WHEN com.checkpoint.api.enums.Priority.MEDIUM THEN 2
                       WHEN com.checkpoint.api.enums.Priority.LOW    THEN 1
                       ELSE 0
                     END DESC
            """)
    Page<Backlog> findByUserIdWithVideoGameOrderByPriorityDesc(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Returns the user's backlog ordered by priority ascending: NULL → LOW → MEDIUM → HIGH.
     * Matches {@code ?sort=priority,asc}. Pageable's own sort is ignored.
     */
    @Query("""
            SELECT b FROM Backlog b
            JOIN FETCH b.videoGame
            WHERE b.user.id = :userId
            ORDER BY CASE b.priority
                       WHEN com.checkpoint.api.enums.Priority.HIGH   THEN 3
                       WHEN com.checkpoint.api.enums.Priority.MEDIUM THEN 2
                       WHEN com.checkpoint.api.enums.Priority.LOW    THEN 1
                       ELSE 0
                     END
            """)
    Page<Backlog> findByUserIdWithVideoGameOrderByPriorityAsc(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Deletes a backlog entry by user ID and video game ID.
     */
    void deleteByUserIdAndVideoGameId(UUID userId, UUID videoGameId);

    /**
     * Counts the number of users who have a specific game in their backlog.
     */
    long countByVideoGameId(UUID videoGameId);

    /**
     * Returns the games appearing in the most users' backlogs, ranked by descending count.
     * Joins through to the rates table so the {@link GameCardDto} projection includes
     * the average rating and rating count in a single query (avoids N+1).
     *
     * @param pageable pagination parameters (used to cap the result size; sort is ignored)
     * @return a page of game cards ordered by backlog count (descending)
     */
    @Query("""
            SELECT new com.checkpoint.api.dto.catalog.GameCardDto(
                vg.id,
                vg.title,
                vg.coverUrl,
                vg.releaseDate,
                vg.averageRating,
                COUNT(DISTINCT r.id)
            )
            FROM Backlog b
            JOIN b.videoGame vg
            LEFT JOIN vg.rates r
            GROUP BY vg.id, vg.title, vg.coverUrl, vg.releaseDate, vg.averageRating
            ORDER BY COUNT(DISTINCT b.id) DESC
            """)
    Page<GameCardDto> findMostBackloggedGames(Pageable pageable);
}
