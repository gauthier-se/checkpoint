package com.checkpoint.api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.checkpoint.api.entities.NewsImportSettings;

/**
 * Repository for the single {@link NewsImportSettings} row.
 */
public interface NewsImportSettingsRepository extends JpaRepository<NewsImportSettings, UUID> {

    /**
     * Returns the settings row, or empty on an installation where no admin has
     * saved the form yet. Ordering by id keeps the choice deterministic should a
     * second row ever be created by hand.
     *
     * @return the first settings row, if any
     */
    Optional<NewsImportSettings> findFirstByOrderByIdAsc();
}
