package com.seyzeriat.desktop.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.seyzeriat.desktop.dto.NewsRequestPayload;
import com.seyzeriat.desktop.dto.NewsResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.NewsService;

/**
 * API client implementation for {@link NewsService}.
 * Handles HTTP communication with the backend to manage news articles.
 */
public class NewsApiClient extends BaseApiClient implements NewsService {

    /**
     * Constructs a new NewsApiClient with the specified authentication service.
     *
     * @param authService the authentication service to use for securing requests
     */
    public NewsApiClient(AuthenticationService authService) {
        super(authService);
    }

    /**
     * Retrieves a paginated list of news articles.
     *
     * @param page the page number to retrieve
     * @param size the maximum number of items per page
     * @param sort the sorting criteria for the news
     * @return a paged response containing news results
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public PagedResponse<NewsResult> getNews(int page, int size, String sort) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/news?page=" + page + "&size=" + size + "&sort=" + sort;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<NewsResult>>() {});
    }

    /**
     * Creates a new news article.
     *
     * @param payload the payload containing the details for the new news article
     * @return the newly created news result
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public NewsResult createNews(NewsRequestPayload payload) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/news";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 201 && response.statusCode() != 200) {
            throw new IOException("Failed to create news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), NewsResult.class);
    }

    /**
     * Updates an existing news article.
     *
     * @param id the unique identifier of the news article to update
     * @param payload the payload containing the updated details
     * @return the updated news result
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public NewsResult updateNews(String id, NewsRequestPayload payload) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/news/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to update news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), NewsResult.class);
    }

    /**
     * Deletes a specific news article.
     *
     * @param id the unique identifier of the news article to delete
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public void deleteNews(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/news/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to delete news with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Publishes a specific news article.
     *
     * @param id the unique identifier of the news article to publish
     * @return the published news result
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public NewsResult publishNews(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/news/" + id + "/publish";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to publish news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), NewsResult.class);
    }

    /**
     * Unpublishes a specific news article.
     *
     * @param id the unique identifier of the news article to unpublish
     * @return the unpublished news result
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public NewsResult unpublishNews(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/news/" + id + "/unpublish";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to unpublish news with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), NewsResult.class);
    }
}
