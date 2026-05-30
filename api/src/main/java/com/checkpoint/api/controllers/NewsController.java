package com.checkpoint.api.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.dto.catalog.NewsSearchCriteria;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.services.NewsSearchService;
import com.checkpoint.api.services.NewsService;

/**
 * REST controller for public news endpoints.
 * Provides read-only access to published news articles with full-text search and filtering.
 */
@Tag(name = "News", description = "Game news feed")
@RestController
@RequestMapping("/api/news")
public class NewsController {

    private static final Logger log = LoggerFactory.getLogger(NewsController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private static final int QUICK_SEARCH_DEFAULT_LIMIT = 5;
    private static final int QUICK_SEARCH_MAX_LIMIT = 10;

    private final NewsService newsService;
    private final NewsSearchService newsSearchService;

    public NewsController(NewsService newsService, NewsSearchService newsSearchService) {
        this.newsService = newsService;
        this.newsSearchService = newsSearchService;
    }

    /**
     * Retrieves a paginated list of published news articles with optional filtering and fuzzy text search.
     *
     * @param page          0-based page number
     * @param size          page size (clamped to [1, 100])
     * @param sort          "publishedAt,desc" | "publishedAt,asc" | "title,asc" | "title,desc" | "relevance"
     * @param q             optional fuzzy query against title + description
     * @param source        optional filter by news origin
     * @param feedName      optional filter by feed name (exact keyword)
     * @param videoGameId   optional filter by linked game
     * @param publishedFrom optional inclusive lower bound on publication date
     * @param publishedTo   optional inclusive upper bound on publication date
     * @return the paginated published news articles
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<NewsResponseDto>> getPublishedNews(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) NewsSource source,
            @RequestParam(required = false) String feedName,
            @RequestParam(required = false) UUID videoGameId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishedTo) {

        log.info("GET /api/news - page: {}, size: {}, sort: {}, q: '{}', source: {}, feedName: '{}', videoGameId: {}, publishedFrom: {}, publishedTo: {}",
                page, size, sort, q, source, feedName, videoGameId, publishedFrom, publishedTo);

        if (publishedFrom != null && publishedTo != null && publishedFrom.isAfter(publishedTo)) {
            throw new IllegalArgumentException("publishedFrom must not be after publishedTo");
        }

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        NewsSearchCriteria criteria = new NewsSearchCriteria(
                q, source, feedName, videoGameId, publishedFrom, publishedTo, sort
        );
        Page<NewsResponseDto> newsPage = newsSearchService.search(criteria, pageable);

        return ResponseEntity.ok(PagedResponseDto.from(newsPage));
    }

    /**
     * Fuzzy quick-search for the global Ctrl+K palette. Returns up to {@code limit} (max 10) results.
     *
     * @param q     the search query
     * @param limit the maximum number of results (clamped to [1, 10])
     * @return a list of matching news articles, sorted by relevance
     */
    @GetMapping("/search")
    public ResponseEntity<List<NewsResponseDto>> quickSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "" + QUICK_SEARCH_DEFAULT_LIMIT) int limit) {

        int clampedLimit = Math.max(1, Math.min(limit, QUICK_SEARCH_MAX_LIMIT));
        log.info("GET /api/news/search - q: '{}', limit: {}", q, clampedLimit);

        return ResponseEntity.ok(newsSearchService.quickSearch(q, clampedLimit));
    }

    /**
     * Retrieves a single published news article by ID.
     *
     * @param newsId the news article ID
     * @return the published news article
     */
    @GetMapping("/{newsId}")
    public ResponseEntity<NewsResponseDto> getNewsById(@PathVariable UUID newsId) {
        log.info("GET /api/news/{}", newsId);

        NewsResponseDto news = newsService.getNewsById(newsId);

        return ResponseEntity.ok(news);
    }
}
