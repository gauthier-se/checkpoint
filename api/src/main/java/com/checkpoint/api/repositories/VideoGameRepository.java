package com.checkpoint.api.repositories;

import java.time.LocalDateTime;
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
import com.checkpoint.api.entities.VideoGame;

/**
 * Repository for VideoGame entity.
 */
@Repository
public interface VideoGameRepository extends JpaRepository<VideoGame, UUID>, VideoGameRepositoryCustom {

    /**
     * Finds a video game by its IGDB external ID.
     * Used for duplicate detection during import.
     *
     * @param igdbId the IGDB game ID
     * @return Optional containing the video game if found
     */
    Optional<VideoGame> findByIgdbId(Long igdbId);

    /**
     * Checks if a video game with the given IGDB ID exists.
     *
     * @param igdbId the IGDB game ID
     * @return true if exists, false otherwise
     */
    boolean existsByIgdbId(Long igdbId);

    /**
     * Fetches every video game whose IGDB ID is in the given collection.
     * Used by the Steam library sync to bulk-resolve matched IGDB IDs in a single query.
     *
     * @param igdbIds the IGDB game IDs to look up
     * @return the matching video games (may be empty)
     */
    List<VideoGame> findAllByIgdbIdIn(Collection<Long> igdbIds);

    /**
     * Fetches a paginated list of games as GameCardDto projections.
     * Uses a single query with aggregated rating calculation to avoid N+1 issues.
     *
     * @param pageable pagination and sorting parameters
     * @return page of GameCardDto
     */
    @Query("""
            SELECT new com.checkpoint.api.dto.catalog.GameCardDto(
                vg.id,
                vg.title,
                vg.coverUrl,
                vg.releaseDate,
                vg.averageRating,
                COUNT(r.id)
            )
            FROM VideoGame vg
            LEFT JOIN vg.rates r
            GROUP BY vg.id, vg.title, vg.coverUrl, vg.releaseDate, vg.averageRating
            """)
    Page<GameCardDto> findAllAsGameCards(Pageable pageable);

    /**
     * Fetches a video game with all its relationships eagerly loaded.
     * Uses JOIN FETCH to avoid N+1 issues when accessing genres, platforms, and companies.
     *
     * @param id the video game ID
     * @return Optional containing the video game with relationships loaded
     */
    @Query("""
            SELECT vg FROM VideoGame vg
            LEFT JOIN FETCH vg.genres
            LEFT JOIN FETCH vg.platforms
            LEFT JOIN FETCH vg.companies
            WHERE vg.id = :id
            """)
    Optional<VideoGame> findByIdWithRelationships(@Param("id") UUID id);

    /**
     * Counts the number of ratings for a video game.
     *
     * @param videoGameId the video game ID
     * @return count of ratings
     */
    @Query("SELECT COUNT(r) FROM Rate r WHERE r.videoGame.id = :videoGameId")
    Long countRatings(@Param("videoGameId") UUID videoGameId);

    /**
     * Counts the number of parent games (DLCs and expansions excluded).
     *
     * @return the count of standalone games
     */
    @Query("SELECT COUNT(vg) FROM VideoGame vg WHERE vg.parentGame IS NULL")
    long countParentGames();

    /**
     * Finds trending games ranked by weighted recent activity score.
     * Uses correlated subqueries to count recent library additions, play sessions,
     * ratings, reviews, likes, and wishlist additions within the given time window.
     * Falls back to all-time average rating and release date when recent activity is low.
     * DLCs are excluded from results.
     *
     * @param since the start of the trending window (typically 7 days ago)
     * @param limit the maximum number of results to return
     * @return a list of trending game data as Object arrays
     *         (id, title, coverUrl, releaseDate, averageRating, ratingCount)
     */
    @Query(value = """
            SELECT vg.id, vg.title, vg.cover_url, vg.release_date, vg.average_rating,
                   (SELECT COUNT(*) FROM rates r WHERE r.video_game_id = vg.id) AS rating_count,
                   (3 * (SELECT COUNT(*) FROM user_games ug WHERE ug.video_game_id = vg.id AND ug.created_at >= :since)
                    + 3 * (SELECT COUNT(*) FROM user_game_plays gp WHERE gp.video_game_id = vg.id AND gp.created_at >= :since)
                    + 2 * (SELECT COUNT(*) FROM rates rr WHERE rr.video_game_id = vg.id AND rr.created_at >= :since)
                    + 2 * (SELECT COUNT(*) FROM reviews rv WHERE rv.video_game_id = vg.id AND rv.created_at >= :since)
                    + 1 * (SELECT COUNT(*) FROM likes lk WHERE lk.video_game_id = vg.id AND lk.created_at >= :since)
                    + 1 * (SELECT COUNT(*) FROM wishes ws WHERE ws.video_game_id = vg.id AND ws.created_at >= :since)
                   ) AS trending_score
            FROM video_games vg
            WHERE vg.parent_game_id IS NULL
            ORDER BY trending_score DESC, COALESCE(vg.average_rating, 0) DESC, vg.release_date DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTrendingGames(@Param("since") LocalDateTime since, @Param("limit") int limit);

    /**
     * Finds trending games among the given set of friend user IDs.
     * Uses the same weighted scoring as {@link #findTrendingGames} but filters
     * all activity subqueries to only count actions from the specified friends.
     *
     * @param friendIds the IDs of users in the follow graph
     * @param since     the start of the trending window
     * @param limit     the maximum number of results to return
     * @return a list of trending game data as Object arrays
     *         (id, title, coverUrl, releaseDate, averageRating, ratingCount)
     */
    @Query(value = """
            SELECT vg.id, vg.title, vg.cover_url, vg.release_date, vg.average_rating,
                   (SELECT COUNT(*) FROM rates r WHERE r.video_game_id = vg.id) AS rating_count,
                   (3 * (SELECT COUNT(*) FROM user_games ug WHERE ug.video_game_id = vg.id AND ug.created_at >= :since AND ug.user_id IN (:friendIds))
                    + 3 * (SELECT COUNT(*) FROM user_game_plays gp WHERE gp.video_game_id = vg.id AND gp.created_at >= :since AND gp.user_id IN (:friendIds))
                    + 2 * (SELECT COUNT(*) FROM rates rr WHERE rr.video_game_id = vg.id AND rr.created_at >= :since AND rr.user_id IN (:friendIds))
                    + 2 * (SELECT COUNT(*) FROM reviews rv WHERE rv.video_game_id = vg.id AND rv.created_at >= :since AND rv.user_id IN (:friendIds))
                    + 1 * (SELECT COUNT(*) FROM likes lk WHERE lk.video_game_id = vg.id AND lk.created_at >= :since AND lk.user_id IN (:friendIds))
                    + 1 * (SELECT COUNT(*) FROM wishes ws WHERE ws.video_game_id = vg.id AND ws.created_at >= :since AND ws.user_id IN (:friendIds))
                   ) AS trending_score
            FROM video_games vg
            WHERE vg.parent_game_id IS NULL
            ORDER BY trending_score DESC, COALESCE(vg.average_rating, 0) DESC, vg.release_date DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findFriendsTrendingGames(
            @Param("friendIds") List<UUID> friendIds,
            @Param("since") LocalDateTime since,
            @Param("limit") int limit);
}
