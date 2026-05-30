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

public class NewsApiClient extends BaseApiClient implements NewsService {

    public NewsApiClient(AuthenticationService authService) {
        super(authService);
    }

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
