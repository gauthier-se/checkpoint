package com.checkpoint.api.controllers;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.catalog.NewsRequestDto;
import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.services.NewsImportService;
import com.checkpoint.api.services.NewsService;

/**
 * REST controller for admin news management operations.
 * All endpoints require the {@code ROLE_ADMIN} authority.
 */
@Tag(name = "Admin", description = "Admin: news management")
@RestController
@RequestMapping("/api/admin/news")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsController {

    private static final Logger log = LoggerFactory.getLogger(AdminNewsController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt,desc";

    private final NewsService newsService;
    private final NewsImportService newsImportService;

    /**
     * Constructs a new AdminNewsController.
     *
     * @param newsService       the news service
     * @param newsImportService the news import orchestrator (Steam / RSS)
     */
    public AdminNewsController(NewsService newsService, NewsImportService newsImportService) {
        this.newsService = newsService;
        this.newsImportService = newsImportService;
    }

    /**
     * Creates a new news article as a draft.
     *
     * @param userDetails the authenticated admin
     * @param request     the news creation request
     * @return the created news article with 201 status
     */
    @PostMapping
    public ResponseEntity<NewsResponseDto> createNews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody NewsRequestDto request) {

        log.info("Admin request: creating news '{}' by {}", request.title(), userDetails.getUsername());

        NewsResponseDto news = newsService.createNews(userDetails.getUsername(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(news);
    }

    /**
     * Retrieves a paginated list of all news articles (drafts + published).
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @param sort the sorting parameters
     * @return the paginated news articles
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<NewsResponseDto>> getAllNews(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("Admin request: fetching all news. Page: {}, Size: {}, Sort: {}", page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        Page<NewsResponseDto> newsPage = newsService.getAllNews(pageable);

        return ResponseEntity.ok(PagedResponseDto.from(newsPage));
    }

    /**
     * Retrieves a single news article by ID (any status).
     *
     * @param newsId the news article ID
     * @return the news article
     */
    @GetMapping("/{newsId}")
    public ResponseEntity<NewsResponseDto> getNewsById(@PathVariable UUID newsId) {
        log.info("Admin request: fetching news with id {}", newsId);

        NewsResponseDto news = newsService.getNewsByIdAdmin(newsId);

        return ResponseEntity.ok(news);
    }

    /**
     * Updates a news article's title, description, and picture.
     *
     * @param newsId  the news article ID
     * @param request the update request
     * @return the updated news article
     */
    @PutMapping("/{newsId}")
    public ResponseEntity<NewsResponseDto> updateNews(
            @PathVariable UUID newsId,
            @RequestBody NewsRequestDto request) {

        log.info("Admin request: updating news with id {}", newsId);

        NewsResponseDto news = newsService.updateNews(newsId, request);

        return ResponseEntity.ok(news);
    }

    /**
     * Deletes a news article.
     *
     * @param newsId the news article ID
     * @return 204 No Content if successful
     */
    @DeleteMapping("/{newsId}")
    public ResponseEntity<Void> deleteNews(@PathVariable UUID newsId) {
        log.info("Admin request: deleting news with id {}", newsId);

        newsService.deleteNews(newsId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Publishes a news article.
     *
     * @param newsId the news article ID
     * @return the published news article
     */
    @PostMapping("/{newsId}/publish")
    public ResponseEntity<NewsResponseDto> publishNews(@PathVariable UUID newsId) {
        log.info("Admin request: publishing news with id {}", newsId);

        NewsResponseDto news = newsService.publishNews(newsId);

        return ResponseEntity.ok(news);
    }

    /**
     * Unpublishes a news article.
     *
     * @param newsId the news article ID
     * @return the unpublished news article
     */
    @PostMapping("/{newsId}/unpublish")
    public ResponseEntity<NewsResponseDto> unpublishNews(@PathVariable UUID newsId) {
        log.info("Admin request: unpublishing news with id {}", newsId);

        NewsResponseDto news = newsService.unpublishNews(newsId);

        return ResponseEntity.ok(news);
    }

    /**
     * Triggers a news import pass for the given external source on demand. Used by
     * admins to top up the news section without waiting for the scheduled task, and by
     * QA when validating the import path.
     *
     * @param source the source to import from (STEAM or RSS — MANUAL is rejected)
     * @return a JSON object {@code {"imported": <count>}}
     */
    @PostMapping("/import/{source}")
    public ResponseEntity<Map<String, Integer>> triggerImport(@PathVariable NewsSource source) {
        log.info("Admin request: triggering news import from {}", source);
        int imported = newsImportService.importFromSource(source);
        return ResponseEntity.ok(Map.of("imported", imported));
    }

    /**
     * Creates a Pageable from the sort string.
     *
     * @param page the page number
     * @param size the page size
     * @param sort the sort string
     * @return a Pageable instance
     */
    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1
                && sortParts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String mappedField = mapSortField(sortField);

        return PageRequest.of(page, size, Sort.by(direction, mappedField));
    }

    /**
     * Maps API sort field names to entity field names.
     *
     * @param field the API field name
     * @return the entity field name
     */
    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "publishedat", "published_at" -> "publishedAt";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "createdAt";
        };
    }
}
