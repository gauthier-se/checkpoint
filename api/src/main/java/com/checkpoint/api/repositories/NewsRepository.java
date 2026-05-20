package com.checkpoint.api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.checkpoint.api.entities.News;
import com.checkpoint.api.entities.NewsSource;

/**
 * Repository for {@link News} entities.
 */
public interface NewsRepository extends JpaRepository<News, UUID> {

    /**
     * Returns true when a news entry already exists with the given source/external-id
     * pair. Used by the news import task to skip duplicates without re-saving them.
     *
     * @param source     the news origin
     * @param externalId the feed item GUID / Steam {@code gid}
     * @return true when a row matches
     */
    boolean existsBySourceAndExternalId(NewsSource source, String externalId);


    /**
     * Finds all published news ordered by publication date descending.
     *
     * @param pageable pagination parameters
     * @return page of published news
     */
    Page<News> findByPublishedAtIsNotNullOrderByPublishedAtDesc(Pageable pageable);

    /**
     * Finds all news ordered by creation date descending (admin view).
     *
     * @param pageable pagination parameters
     * @return page of all news
     */
    Page<News> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Finds a single published news article by ID.
     *
     * @param id the news ID
     * @return the published news, or empty if not found or not published
     */
    Optional<News> findByIdAndPublishedAtIsNotNull(UUID id);
}
