package com.checkpoint.api.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.NewsRequestDto;
import com.checkpoint.api.dto.catalog.NewsResponseDto;

/**
 * Service for managing news articles.
 */
public interface NewsService {

    /**
     * Creates a new news article as a draft (unpublished).
     *
     * @param userEmail the authenticated admin's email
     * @param request   the news creation request
     * @return the created news article
     */
    NewsResponseDto createNews(String userEmail, NewsRequestDto request);

    /**
     * Updates an existing news article's title, description, and picture.
     *
     * @param newsId  the news article ID
     * @param request the update request
     * @return the updated news article
     */
    NewsResponseDto updateNews(UUID newsId, NewsRequestDto request);

    /**
     * Deletes a news article.
     *
     * @param newsId the news article ID
     */
    void deleteNews(UUID newsId);

    /**
     * Publishes a news article by setting its publishedAt timestamp.
     *
     * @param newsId the news article ID
     * @return the published news article
     */
    NewsResponseDto publishNews(UUID newsId);

    /**
     * Unpublishes a news article by clearing its publishedAt timestamp.
     *
     * @param newsId the news article ID
     * @return the unpublished news article
     */
    NewsResponseDto unpublishNews(UUID newsId);

    /**
     * Returns a single published news article by ID.
     *
     * @param newsId the news article ID
     * @return the published news article
     */
    NewsResponseDto getNewsById(UUID newsId);

    /**
     * Returns a paginated list of all news articles (drafts + published) for admin view.
     *
     * @param pageable pagination parameters
     * @return page of all news articles
     */
    Page<NewsResponseDto> getAllNews(Pageable pageable);

    /**
     * Returns a single news article by ID regardless of publication status (admin view).
     *
     * @param newsId the news article ID
     * @return the news article
     */
    NewsResponseDto getNewsByIdAdmin(UUID newsId);
}
