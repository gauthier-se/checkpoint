package com.checkpoint.api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.VideoGame;

/**
 * Repository for VideoGame entity.
 */
@Repository
public interface VideoGameRepository extends JpaRepository<VideoGame, UUID> {

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
}
