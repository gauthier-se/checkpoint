package com.seyzeriat.desktop.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.dto.ReviewReportResult;
import com.seyzeriat.desktop.dto.ReviewResult;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.ReviewService;

public class ReviewApiClient extends BaseApiClient implements ReviewService {

    public ReviewApiClient(AuthenticationService authService) {
        super(authService);
    }

    @Override
    public PagedResponse<ReviewResult> getReviews(int page, int size) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/reviews?page=" + page + "&size=" + size;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch reviews with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<ReviewResult>>() {});
    }

    @Override
    public PagedResponse<ReviewResult> getReportedReviews(int page, int size) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/reviews/reported?page=" + page + "&size=" + size;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch reported reviews with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<ReviewResult>>() {});
    }

    @Override
    public PagedResponse<ReviewReportResult> getReviewReports(String reviewId, int page, int size)
            throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/reviews/" + reviewId + "/reports?page=" + page + "&size=" + size;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch review reports with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<ReviewReportResult>>() {});
    }

    @Override
    public void deleteReview(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/reviews/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to delete review with status " + response.statusCode() + ": " + response.body());
        }
    }

    @Override
    public void deleteComment(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/comments/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to delete comment with status " + response.statusCode() + ": " + response.body());
        }
    }
}
