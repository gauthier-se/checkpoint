package com.checkpoint.api.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.GameCardDto;

/**
 * Custom repository interface for dynamic filtering of video games.
 * Provides methods that build JPQL queries dynamically based on optional filter parameters.
 */
public interface VideoGameRepositoryCustom {

    /**
     * Fetches a paginated list of games as GameCardDto projections with optional filters.
     * When no filters are provided, behavior is identical to the standard findAllAsGameCards query.
     * Multiple genres / platforms are combined with OR semantics (a game matches if it has
     * any of the requested genres), while different facets are combined with AND.
     *
     * @param pageable   pagination and sorting parameters
     * @param genres     optional genre name filters (case-insensitive; matches any)
     * @param platforms  optional platform name filters (case-insensitive; matches any)
     * @param yearMin    optional minimum release year (inclusive)
     * @param yearMax    optional maximum release year (inclusive)
     * @param ratingMin  optional minimum average rating (inclusive)
     * @param ratingMax  optional maximum average rating (inclusive)
     * @return page of GameCardDto matching the filters
     */
    Page<GameCardDto> findAllAsGameCardsWithFilters(Pageable pageable,
                                                     List<String> genres,
                                                     List<String> platforms,
                                                     Integer yearMin,
                                                     Integer yearMax,
                                                     Double ratingMin,
                                                     Double ratingMax);
}
