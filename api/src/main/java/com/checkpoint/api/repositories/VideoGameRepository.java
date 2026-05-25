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
     * Checks if any video game has the given title (case-insensitive).
     * Used by the admin create-game flow to prevent silent duplicates.
     */
    boolean existsByTitleIgnoreCase(String title);

    /**
     * Checks if any video game other than the one with the given ID has the
     * given title (case-insensitive). Used by the admin update-game flow to
     * prevent collisions while still allowing a no-op title save.
     */
    @Query("""
            SELECT CASE WHEN COUNT(vg) > 0 THEN TRUE ELSE FALSE END
            FROM VideoGame vg
            WHERE LOWER(vg.title) = LOWER(:title) AND vg.id <> :excludedId
            """)
    boolean existsByTitleIgnoreCaseAndIdNot(@Param("title") String title, @Param("excludedId") UUID excludedId);

    /**
     * Fetches every video game whose IGDB ID is in the given collection.
     * Used by the Steam library sync to bulk-resolve matched IGDB IDs in a single query.
     *
     * @param igdbIds the IGDB game IDs to look up
     * @return the matching video games (may be empty)
     */
    List<VideoGame> findAllByIgdbIdIn(Collection<Long> igdbIds);

    /**
     * Returns distinct video games that are in at least one user library. Used by the
     * news import task to limit the Steam news pass to games that someone actually
     * cares about.
     *
     * @return the matching video games (may be empty)
     */
    @Query("""
            SELECT DISTINCT ug.videoGame FROM UserGame ug
            """)
    List<VideoGame> findGamesWithAtLeastOneUserLink();

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
     * Batched counterpart of {@link #findByIdWithRelationships} — loads every game whose
     * ID is in the given collection and eagerly fetches genres, platforms, and companies
     * in a single round-trip. Used by the recommendation service to score either the
     * user's library or the candidate pool without N+1 access on tag sets.
     *
     * @param ids the video game IDs to load
     * @return the matching games with relationships hydrated (may be empty)
     */
    @Query("""
            SELECT DISTINCT vg FROM VideoGame vg
            LEFT JOIN FETCH vg.genres
            LEFT JOIN FETCH vg.platforms
            LEFT JOIN FETCH vg.companies
            WHERE vg.id IN :ids
            """)
    List<VideoGame> findAllByIdInWithRelationships(@Param("ids") Collection<UUID> ids);

    /**
     * Pre-filters the catalog to games sharing at least one genre or company with the
     * user's taste profile, excluding any DLC and any game the user already has a
     * relationship with — library ({@code UserGame}), wishlist ({@code Wish}),
     * favorites ({@code Favorite}), or a top-level game-like ({@code Like}). Returns
     * only IDs so the caller can apply an upper bound via
     * {@link org.springframework.data.domain.Pageable} without hitting Hibernate's
     * JOIN FETCH + first/max in-memory pagination caveat — load the entities in a
     * second pass through {@link #findAllByIdInWithRelationships}.
     *
     * <p>Used by the v1 recommendation service to keep the candidate pool bounded
     * (typically a few hundred rows out of the full catalog).</p>
     *
     * @param userId          the authenticated user's ID
     * @param likedGenreIds   genre IDs that appear in the user's affinity profile
     * @param likedCompanyIds company IDs that appear in the user's affinity profile
     * @param pageable        caps the candidate set (sort is ignored)
     * @return up to {@code pageable.pageSize} candidate game IDs
     */
    @Query("""
            SELECT vg.id FROM VideoGame vg
            LEFT JOIN vg.genres g
            LEFT JOIN vg.companies c
            WHERE vg.parentGame IS NULL
              AND (g.id IN :likedGenreIds OR c.id IN :likedCompanyIds)
              AND vg.id NOT IN (
                  SELECT ug.videoGame.id FROM UserGame ug WHERE ug.user.id = :userId
              )
              AND vg.id NOT IN (
                  SELECT w.videoGame.id FROM Wish w WHERE w.user.id = :userId
              )
              AND vg.id NOT IN (
                  SELECT f.videoGame.id FROM Favorite f WHERE f.user.id = :userId
              )
              AND vg.id NOT IN (
                  SELECT l.videoGame.id FROM Like l WHERE l.user.id = :userId AND l.videoGame IS NOT NULL
              )
            GROUP BY vg.id
            """)
    List<UUID> findCandidateIdsForRecommendation(
            @Param("userId") UUID userId,
            @Param("likedGenreIds") Collection<UUID> likedGenreIds,
            @Param("likedCompanyIds") Collection<UUID> likedCompanyIds,
            Pageable pageable);

    /**
     * Pre-filters the catalog to games similar to a single seed game — those sharing at
     * least one genre or company with it — excluding the seed game itself and any DLC
     * ({@code parentGame IS NULL}). When the viewer is authenticated, also excludes any
     * game they already have a relationship with — library ({@code UserGame}), wishlist
     * ({@code Wish}), favorites ({@code Favorite}), or a top-level game-like
     * ({@code Like}). Anonymous callers pass a sentinel {@code viewerId} that matches no
     * user, so the four {@code NOT IN} clauses leave every game in place.
     *
     * <p>Item-to-item counterpart of {@link #findCandidateIdsForRecommendation}. Returns
     * only IDs so the caller can cap the pool via {@link Pageable} and hydrate the
     * entities in a second pass through {@link #findAllByIdInWithRelationships}.</p>
     *
     * @param seedGameId the game whose neighbours are sought
     * @param viewerId   the authenticated viewer's ID, or a sentinel UUID when anonymous
     * @param genreIds   genre IDs of the seed game
     * @param companyIds company IDs of the seed game
     * @param pageable   caps the candidate set (sort is ignored)
     * @return up to {@code pageable.pageSize} candidate game IDs
     */
    @Query("""
            SELECT vg.id FROM VideoGame vg
            LEFT JOIN vg.genres g
            LEFT JOIN vg.companies c
            WHERE vg.parentGame IS NULL
              AND vg.id <> :seedGameId
              AND (g.id IN :genreIds OR c.id IN :companyIds)
              AND vg.id NOT IN (
                  SELECT ug.videoGame.id FROM UserGame ug WHERE ug.user.id = :viewerId
              )
              AND vg.id NOT IN (
                  SELECT w.videoGame.id FROM Wish w WHERE w.user.id = :viewerId
              )
              AND vg.id NOT IN (
                  SELECT f.videoGame.id FROM Favorite f WHERE f.user.id = :viewerId
              )
              AND vg.id NOT IN (
                  SELECT l.videoGame.id FROM Like l WHERE l.user.id = :viewerId AND l.videoGame IS NOT NULL
              )
            GROUP BY vg.id
            """)
    List<UUID> findSimilarCandidateIds(
            @Param("seedGameId") UUID seedGameId,
            @Param("viewerId") UUID viewerId,
            @Param("genreIds") Collection<UUID> genreIds,
            @Param("companyIds") Collection<UUID> companyIds,
            Pageable pageable);

    /**
     * Loads {@link GameCardDto} projections (with accurate rating counts) for the given
     * IDs in a single aggregated query. Used by the similarity service to materialise the
     * final, already-ranked top-N selection; the caller restores the ranked order since
     * SQL {@code IN} does not preserve it.
     *
     * @param ids the video game IDs to load
     * @return the matching game cards (may be empty, order unspecified)
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
            WHERE vg.id IN :ids
            GROUP BY vg.id, vg.title, vg.coverUrl, vg.releaseDate, vg.averageRating
            """)
    List<GameCardDto> findGameCardsByIdIn(@Param("ids") Collection<UUID> ids);

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

    /**
     * Paginated counterpart of {@link #findFriendsTrendingGames}. Restricts results to
     * games with at least one interaction from the follow graph within the window so
     * deeper pages don't drift into unrelated games sorted only by average rating.
     *
     * @param friendIds the IDs of users in the follow graph
     * @param since     the start of the trending window
     * @param pageable  pagination parameters (sorting is ignored — server-side score order)
     * @return a page of trending game data as Object arrays
     *         (id, title, coverUrl, releaseDate, averageRating, ratingCount)
     */
    @Query(value = """
            SELECT * FROM (
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
            ) t
            WHERE t.trending_score > 0
            ORDER BY t.trending_score DESC, COALESCE(t.average_rating, 0) DESC, t.release_date DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM video_games vg
            WHERE vg.parent_game_id IS NULL
              AND vg.id IN (
                SELECT ug.video_game_id FROM user_games ug WHERE ug.user_id IN (:friendIds) AND ug.created_at >= :since
                UNION
                SELECT gp.video_game_id FROM user_game_plays gp WHERE gp.user_id IN (:friendIds) AND gp.created_at >= :since
                UNION
                SELECT rr.video_game_id FROM rates rr WHERE rr.user_id IN (:friendIds) AND rr.created_at >= :since
                UNION
                SELECT rv.video_game_id FROM reviews rv WHERE rv.user_id IN (:friendIds) AND rv.created_at >= :since
                UNION
                SELECT lk.video_game_id FROM likes lk WHERE lk.user_id IN (:friendIds) AND lk.created_at >= :since
                UNION
                SELECT ws.video_game_id FROM wishes ws WHERE ws.user_id IN (:friendIds) AND ws.created_at >= :since
              )
            """,
            nativeQuery = true)
    Page<Object[]> findFriendsTrendingGamesPage(
            @Param("friendIds") List<UUID> friendIds,
            @Param("since") LocalDateTime since,
            Pageable pageable);
}
