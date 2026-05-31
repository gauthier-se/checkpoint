package com.seyzeriat.desktop.service;

import java.io.IOException;
import com.seyzeriat.desktop.dto.NewsResult;
import com.seyzeriat.desktop.dto.NewsRequestPayload;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.exception.UnauthorizedException;

/**
 * Service interface for managing news.
 * Provides operations to create, retrieve, update, delete, and publish news articles.
 */
public interface NewsService {

    /**
     * Retrieves a paginated list of news articles.
     *
     * @param page the page number to retrieve (zero-based)
     * @param size the maximum number of items per page
     * @param sort the sorting criteria for the news
     * @return a paged response containing news results
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    PagedResponse<NewsResult> getNews(int page, int size, String sort) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Creates a new news article.
     *
     * @param payload the payload containing the details for the new news article
     * @return the newly created news result
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    NewsResult createNews(NewsRequestPayload payload) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Updates an existing news article.
     *
     * @param id the unique identifier of the news article to update
     * @param payload the payload containing the updated details
     * @return the updated news result
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    NewsResult updateNews(String id, NewsRequestPayload payload) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Deletes a specific news article.
     *
     * @param id the unique identifier of the news article to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    void deleteNews(String id) throws IOException, InterruptedException, UnauthorizedException;
    /**
     * Publishes a specific news article.
     *
     * @param id the unique identifier of the news article to publish
     * @return the published news result
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    NewsResult publishNews(String id) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Unpublishes a specific news article.
     *
     * @param id the unique identifier of the news article to unpublish
     * @return the unpublished news result
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    NewsResult unpublishNews(String id) throws IOException, InterruptedException, UnauthorizedException;
}
