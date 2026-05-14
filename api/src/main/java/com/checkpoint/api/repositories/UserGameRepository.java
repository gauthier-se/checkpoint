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
     * Returns all games in a user's library (paginated), with video game eagerly fetched.
     */
    @Query("""
            SELECT ug FROM UserGame ug
            JOIN FETCH ug.videoGame
            WHERE ug.user.id = :userId
            """)
    Page<UserGame> findByUserIdWithVideoGame(@Param("userId") UUID userId, Pageable pageable);

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
}
