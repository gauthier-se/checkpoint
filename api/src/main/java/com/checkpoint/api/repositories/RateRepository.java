package com.checkpoint.api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.checkpoint.api.entities.Rate;

/**
 * Repository for {@link Rate} entity.
 */
public interface RateRepository extends JpaRepository<Rate, UUID> {

    /**
     * Finds a user's rate for a specific video game by user email.
     *
     * @param email the user's email
     * @param videoGameId the video game ID
     * @return an optional containing the rate if found
     */
    Optional<Rate> findByUserEmailAndVideoGameId(String email, UUID videoGameId);

    /**
     * Finds a user's rate for a specific video game.
     *
     * @param pseudo the user's pseudo
     * @param videoGameId the video game ID
     * @return an optional containing the rate if found
     */
    Optional<Rate> findByUserPseudoAndVideoGameId(String pseudo, UUID videoGameId);

    /**
     * Finds rates for a specific video game and a list of user IDs.
     *
     * @param videoGameId the video game ID
     * @param userIds list of user IDs
     * @return list of rates
     */
    List<Rate> findByVideoGameIdAndUserIdIn(UUID videoGameId, List<UUID> userIds);

    /**
     * Calculates the average rating for a video game.
     *
     * @param videoGameId the video game ID
     * @return average rating or null if no ratings
     */
    @Query("SELECT AVG(CAST(r.score AS double)) FROM Rate r WHERE r.videoGame.id = :videoGameId")
    Double calculateAverageRating(@Param("videoGameId") UUID videoGameId);

    /**
     * Counts how many ratings reference a given video game.
     * Used by the admin delete-game integrity check.
     */
    long countByVideoGameId(UUID videoGameId);

    /**
     * Returns every rating the given user has authored.
     * Used by the recommendation service to build the user's affinity profile.
     */
    List<Rate> findAllByUserId(UUID userId);
}
