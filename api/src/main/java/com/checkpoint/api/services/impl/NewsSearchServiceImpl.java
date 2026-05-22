package com.checkpoint.api.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.dto.catalog.NewsSearchCriteria;
import com.checkpoint.api.entities.News;
import com.checkpoint.api.mapper.NewsMapper;
import com.checkpoint.api.services.NewsSearchService;

import jakarta.persistence.EntityManager;

/**
 * Implementation of {@link NewsSearchService} using Hibernate Search with Lucene backend.
 * Mirrors {@link GameSearchServiceImpl} but adds filters, range queries, and pagination.
 */
@Service
@Transactional(readOnly = true)
public class NewsSearchServiceImpl implements NewsSearchService {

    private static final Logger log = LoggerFactory.getLogger(NewsSearchServiceImpl.class);

    private static final int FUZZY_MAX_EDIT_DISTANCE = 2;
    private static final int QUICK_SEARCH_MAX_LIMIT = 10;

    private final EntityManager entityManager;
    private final NewsMapper newsMapper;

    public NewsSearchServiceImpl(EntityManager entityManager, NewsMapper newsMapper) {
        this.entityManager = entityManager;
        this.newsMapper = newsMapper;
    }

    @Override
    public Page<NewsResponseDto> search(NewsSearchCriteria criteria, Pageable pageable) {
        log.debug("Searching news - criteria: {}, pageable: {}", criteria, pageable);

        SearchSession searchSession = Search.session(entityManager);

        SearchResult<News> result = searchSession.search(News.class)
                .where(f -> buildPredicate(f, criteria))
                .sort(f -> buildSort(f, criteria))
                .fetch((int) pageable.getOffset(), pageable.getPageSize());

        List<NewsResponseDto> content = result.hits().stream()
                .map(newsMapper::toDto)
                .toList();

        long total = result.total().hitCount();
        log.debug("News search returned {}/{} results", content.size(), total);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<NewsResponseDto> quickSearch(String q, int limit) {
        if (q == null || q.isBlank()) {
            return List.of();
        }

        int clampedLimit = Math.max(1, Math.min(limit, QUICK_SEARCH_MAX_LIMIT));
        log.debug("Quick news search - q: '{}', limit: {}", q, clampedLimit);

        SearchSession searchSession = Search.session(entityManager);

        List<News> hits = searchSession.search(News.class)
                .where(f -> f.match()
                        .fields("title", "description")
                        .matching(q)
                        .fuzzy(FUZZY_MAX_EDIT_DISTANCE))
                .fetchHits(clampedLimit);

        return hits.stream()
                .map(newsMapper::toDto)
                .toList();
    }

    /**
     * Builds the boolean predicate combining the optional fuzzy text query and filters.
     */
    private BooleanPredicateClausesStep<?> buildPredicate(
            SearchPredicateFactory f,
            NewsSearchCriteria criteria
    ) {
        BooleanPredicateClausesStep<?> bool = f.bool();

        if (criteria.hasQuery()) {
            bool.must(f.match()
                    .fields("title", "description")
                    .matching(criteria.q())
                    .fuzzy(FUZZY_MAX_EDIT_DISTANCE));
        } else {
            bool.must(f.matchAll());
        }

        if (criteria.source() != null) {
            bool.filter(f.match().field("source").matching(criteria.source().name()));
        }
        if (criteria.feedName() != null && !criteria.feedName().isBlank()) {
            bool.filter(f.match().field("feedName").matching(criteria.feedName()));
        }
        if (criteria.videoGameId() != null) {
            bool.filter(f.match().field("videoGame.id").matching(criteria.videoGameId()));
        }
        if (criteria.publishedFrom() != null || criteria.publishedTo() != null) {
            LocalDateTime from = criteria.publishedFrom() != null
                    ? criteria.publishedFrom().atStartOfDay()
                    : null;
            LocalDateTime to = criteria.publishedTo() != null
                    ? criteria.publishedTo().atTime(23, 59, 59, 999_999_999)
                    : null;
            if (from != null && to != null) {
                bool.filter(f.range().field("publishedAt").between(from, to));
            } else if (from != null) {
                bool.filter(f.range().field("publishedAt").atLeast(from));
            } else {
                bool.filter(f.range().field("publishedAt").atMost(to));
            }
        }

        return bool;
    }

    /**
     * Builds the sort clause based on criteria.sort() and whether a text query is present.
     * Default: relevance when a query is active, otherwise publishedAt desc.
     */
    private SortFinalStep buildSort(SearchSortFactory f, NewsSearchCriteria criteria) {
        String sort = criteria.sort();

        if (sort == null || sort.isBlank() || "relevance".equalsIgnoreCase(sort)) {
            // Relevance is meaningful only with a text query; fall back to publishedAt desc otherwise.
            return criteria.hasQuery()
                    ? f.score()
                    : f.field("publishedAt").desc();
        }

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        boolean asc = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim());

        String mapped = switch (field.toLowerCase()) {
            case "title" -> "titleSort";
            case "publishedat", "published_at" -> "publishedAt";
            default -> "publishedAt";
        };

        return asc ? f.field(mapped).asc() : f.field(mapped).desc();
    }
}
