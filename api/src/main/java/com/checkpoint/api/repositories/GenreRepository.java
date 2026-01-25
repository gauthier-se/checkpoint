package com.checkpoint.api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.Genre;

/**
 * Repository for Genre entity.
 */
@Repository
public interface GenreRepository extends JpaRepository<Genre, UUID> {

    /**
     * Finds a genre by its name (case-insensitive).
     *
     * @param name the genre name
     * @return Optional containing the genre if found
     */
    Optional<Genre> findByNameIgnoreCase(String name);
}
