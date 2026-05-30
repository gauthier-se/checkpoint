package com.checkpoint.api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.checkpoint.api.dto.profile.RatingDistributionEntryDto;
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
     * Finds rates authored by any of the given users for any of the given video games.
     * Used by the profile comparison feature to resolve both users' ratings for the
     * games they have in common in a single round-trip.
     *
     * @param userIds list of user IDs
     * @param videoGameIds list of video game IDs
     * @return list of matching rates
     */
    List<Rate> findByUserIdInAndVideoGameIdIn(List<UUID> userIds, List<UUID> videoGameIds);

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

    /**
     * Counts the user's "one-star" ratings ({@code score <= 2}, i.e. 0.5 or 1
     * star on the 5-star display). Powers the {@code THE_CAKE_IS_A_LIE} badge,
     * which fires when the count is exactly 13.
     */
    @Query("""
            SELECT COUNT(r) FROM Rate r
            WHERE r.user.id = :userId AND r.score <= 2
            """)
    long countOneStarByUserId(@Param("userId") UUID userId);

    /**
     * Returns true if the user has changed at least one rating's score the given
     * number of times. Powers the {@code INDECISIVE} easter-egg badge.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Rate r
            WHERE r.user.id = :userId AND r.changeCount >= :threshold
            """)
    boolean existsRateChangedAtLeastByUserId(@Param("userId") UUID userId,
                                              @Param("threshold") int threshold);

    /**
     * Returns the distribution of a user's ratings grouped by score. The result
     * is sparse: only scores the user has actually used appear, so callers must
     * fill the missing scores with a zero count when rendering all ten bars.
     *
     * @param userId the user ID
     * @return one entry per used score, in ascending score order
     */
    @Query("""
            SELECT new com.checkpoint.api.dto.profile.RatingDistributionEntryDto(r.score, COUNT(r))
            FROM Rate r
            WHERE r.user.id = :userId
            GROUP BY r.score
            ORDER BY r.score
            """)
    List<RatingDistributionEntryDto> findDistributionByUserId(@Param("userId") UUID userId);

    /**
     * Returns the distribution of a video game's ratings (across all users)
     * grouped by score. The result is sparse, like
     * {@link #findDistributionByUserId(UUID)}.
     *
     * @param videoGameId the video game ID
     * @return one entry per used score, in ascending score order
     */
    @Query("""
            SELECT new com.checkpoint.api.dto.profile.RatingDistributionEntryDto(r.score, COUNT(r))
            FROM Rate r
            WHERE r.videoGame.id = :videoGameId
            GROUP BY r.score
            ORDER BY r.score
            """)
    List<RatingDistributionEntryDto> findDistributionByVideoGameId(@Param("videoGameId") UUID videoGameId);
}
