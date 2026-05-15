package com.checkpoint.api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

}
