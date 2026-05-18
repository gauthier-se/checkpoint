package com.checkpoint.api.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.checkpoint.api.entities.AuthProvider;
import com.checkpoint.api.entities.User;

/**
 * Repository for {@link User} entity.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email address
     * @return an optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists by their email address.
     *
     * @param email the email address
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user exists by their pseudo.
     *
     * @param pseudo the pseudo
     * @return true if exists, false otherwise
     */
    boolean existsByPseudo(String pseudo);

    /**
     * Finds a user previously authenticated via the given OAuth2 provider
     * with the given provider-specific identifier.
     *
     * @param provider   the authentication provider
     * @param providerId the provider-specific user identifier
     * @return an optional containing the user if found
     */
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /**
     * Finds a user by their linked Steam account ID (SteamID64).
     *
     * @param steamId the SteamID64
     * @return an optional containing the user if found
     */
    Optional<User> findBySteamId(String steamId);

    /**
     * Finds Steam-linked users whose cached Steam profile is older than the given cutoff
     * (or has never been synced). Used by the scheduled refresh task.
     *
     * @param cutoff   profiles synced before this instant are considered stale
     * @param pageable pagination parameters (used to cap the batch size)
     * @return a list of users whose Steam profile cache needs refreshing
     */
    @Query("SELECT u FROM User u WHERE u.steamId IS NOT NULL "
            + "AND (u.steamSyncedAt IS NULL OR u.steamSyncedAt < :cutoff) "
            + "ORDER BY u.steamSyncedAt ASC NULLS FIRST")
    List<User> findSteamLinkedUsersStaleBefore(@Param("cutoff") LocalDateTime cutoff,
                                               Pageable pageable);

    /**
     * Finds all users who follow the given user (paginated).
     *
     * @param userId   the ID of the user whose followers to retrieve
     * @param pageable pagination parameters
     * @return a page of users who follow the given user
     */
    @Query("SELECT u FROM User u JOIN u.following f WHERE f.id = :userId")
    Page<User> findFollowersByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Finds all users that the given user follows (paginated).
     *
     * @param userId   the ID of the user whose following list to retrieve
     * @param pageable pagination parameters
     * @return a page of users that the given user follows
     */
    @Query("SELECT f FROM User u JOIN u.following f WHERE u.id = :userId")
    Page<User> findFollowingByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Finds a user by their pseudo (display name).
     *
     * @param pseudo the pseudo
     * @return an optional containing the user if found
     */
    Optional<User> findByPseudo(String pseudo);

    /**
     * Finds a user by their pseudo, eagerly fetching badges.
     *
     * @param pseudo the pseudo
     * @return an optional containing the user with badges loaded
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.badges WHERE u.pseudo = :pseudo")
    Optional<User> findByPseudoWithBadges(@Param("pseudo") String pseudo);

    /**
     * Counts the number of followers for a given user.
     *
     * @param userId the user's ID
     * @return the follower count
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.following f WHERE f.id = :userId")
    long countFollowersByUserId(@Param("userId") UUID userId);

    /**
     * Counts the number of users that the given user follows.
     *
     * @param userId the user's ID
     * @return the following count
     */
    @Query("SELECT COUNT(f) FROM User u JOIN u.following f WHERE u.id = :userId")
    long countFollowingByUserId(@Param("userId") UUID userId);

    /**
     * Checks if a follower is following a target user.
     *
     * @param followerId  the ID of the potential follower
     * @param followingId the ID of the user potentially being followed
     * @return true if the follower is following the target user
     */
    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.following f WHERE u.id = :followerId AND f.id = :followingId")
    boolean isFollowing(@Param("followerId") UUID followerId, @Param("followingId") UUID followingId);

    /**
     * Counts the number of users that are not banned.
     *
     * @return the active (non-banned) user count
     */
    long countByBannedFalse();

    /**
     * Finds users ranked by follower count (descending).
     * Returns each user alongside their follower count.
     *
     * @param pageable pagination parameters
     * @return a page of Object[] where [0] = User, [1] = follower count (Long)
     */
    @Query("SELECT u, COUNT(f) AS fc FROM User u LEFT JOIN u.followers f GROUP BY u ORDER BY fc DESC")
    Page<Object[]> findPopularMembers(Pageable pageable);

    /**
     * Finds users ranked by review count (descending).
     * Returns each user alongside their review count.
     *
     * @param pageable pagination parameters
     * @return a page of Object[] where [0] = User, [1] = review count (Long)
     */
    @Query("SELECT u, COUNT(r) AS rc FROM User u LEFT JOIN u.reviews r GROUP BY u ORDER BY rc DESC")
    Page<Object[]> findTopReviewers(Pageable pageable);

    /**
     * Finds suggested members for a given user based on shared games.
     * Excludes the user themselves and users they already follow.
     * Ranked by the number of shared games (descending).
     *
     * @param userId the authenticated user's ID
     * @param pageable pagination parameters
     * @return a page of Object[] where [0] = User, [1] = shared game count (Long)
     */
    @Query("SELECT u, COUNT(ug.videoGame.id) AS shared FROM User u JOIN u.userGames ug "
            + "WHERE ug.videoGame.id IN (SELECT ug2.videoGame.id FROM UserGame ug2 WHERE ug2.user.id = :userId) "
            + "AND u.id <> :userId "
            + "AND u NOT IN (SELECT f FROM User cu JOIN cu.following f WHERE cu.id = :userId) "
            + "GROUP BY u ORDER BY shared DESC")
    Page<Object[]> findSuggestedMembers(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Searches users by pseudo with case-insensitive partial matching.
     *
     * @param pseudo the search term
     * @param pageable pagination parameters
     * @return a page of matching users
     */
    Page<User> findByPseudoContainingIgnoreCase(String pseudo, Pageable pageable);

    /**
     * Returns non-banned users ordered by XP descending. Used by the public leaderboard.
     *
     * @param pageable pagination parameters (the caller controls the limit)
     * @return a page of users ranked by XP
     */
    @Query("SELECT u FROM User u WHERE u.banned = false ORDER BY u.xpPoint DESC")
    Page<User> findLeaderboardByXp(Pageable pageable);

    /**
     * Returns non-banned users ordered by level descending, breaking ties on XP descending.
     * Used by the public leaderboard.
     *
     * @param pageable pagination parameters (the caller controls the limit)
     * @return a page of users ranked by level, then XP
     */
    @Query("SELECT u FROM User u WHERE u.banned = false ORDER BY u.level DESC, u.xpPoint DESC")
    Page<User> findLeaderboardByLevel(Pageable pageable);

    /**
     * Finds the IDs of all users that the given user follows.
     *
     * @param userId the user's ID
     * @return a list of followed user IDs
     */
    @Query("SELECT f.id FROM User u JOIN u.following f WHERE u.id = :userId")
    List<UUID> findFollowingIdsByUserId(@Param("userId") UUID userId);

    /**
     * Deletes every row from the {@code user_follows} join table that involves
     * the given user, either as follower or as the user being followed.
     * Used when erasing the user, since the inverse side
     * ({@link User#getFollowers()}) is not cascade-managed and would otherwise
     * leave dangling join-table rows that violate the foreign-key constraint.
     *
     * @param userId the user being erased
     */
    @Modifying
    @Query(value = "DELETE FROM user_follows WHERE follower_id = :userId OR following_id = :userId",
           nativeQuery = true)
    void deleteFollowsInvolvingUser(@Param("userId") UUID userId);
}
