package com.checkpoint.api.repositories;

import java.time.LocalDateTime;
import java.util.Collection;
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

    /**
     * Counts the articles pulled in from external sources since the given instant.
     * Backs the daily import ceiling: {@code created_at} is the insertion time, so
     * this is the number of rows the importers actually added today, not the number
     * of items the feeds offered. {@link NewsSource#MANUAL} is left out by the
     * caller, so admin-written news never eats the import budget.
     *
     * @param sources the sources to count
     * @param from    the start of the window, inclusive
     * @return how many matching articles were created in the window
     */
    long countBySourceInAndCreatedAtGreaterThanEqual(Collection<NewsSource> sources, LocalDateTime from);
}
