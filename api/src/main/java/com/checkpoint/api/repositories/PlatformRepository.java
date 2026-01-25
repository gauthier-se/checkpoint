package com.checkpoint.api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.Platform;

/**
 * Repository for Platform entity.
 */
@Repository
public interface PlatformRepository extends JpaRepository<Platform, UUID> {

    /**
     * Finds a platform by its name (case-insensitive).
     *
     * @param name the platform name
     * @return Optional containing the platform if found
     */
    Optional<Platform> findByNameIgnoreCase(String name);
}
