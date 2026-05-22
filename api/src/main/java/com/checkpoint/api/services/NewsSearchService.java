package com.checkpoint.api.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.dto.catalog.NewsSearchCriteria;

/**
 * Full-text search service for news articles, backed by Hibernate Search / Lucene.
 */
public interface NewsSearchService {

    /**
     * Paginated search with optional fuzzy text query, filters, and sort.
     * When the criteria has no text query, this behaves like a paginated listing
     * with Lucene-backed sorting and range filters.
     *
     * @param criteria the filter / sort criteria (nullable fields are ignored)
     * @param pageable pagination parameters
     * @return a page of mapped news DTOs
     */
    Page<NewsResponseDto> search(NewsSearchCriteria criteria, Pageable pageable);

    /**
     * Lightweight fuzzy-only search used by the Ctrl+K palette.
     * Returns up to {@code limit} results (clamped to 10), sorted by relevance.
     *
     * @param q     the search query (required, must be non-blank)
     * @param limit the maximum number of results to return
     * @return a list of mapped news DTOs, empty when {@code q} is blank
     */
    List<NewsResponseDto> quickSearch(String q, int limit);
}
