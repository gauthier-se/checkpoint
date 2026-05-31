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

/**
 * API client implementation for {@link ReviewService}.
 * Handles HTTP communication with the backend to manage reviews.
 */
public class ReviewApiClient extends BaseApiClient implements ReviewService {

    /**
     * Constructs a new ReviewApiClient with the specified authentication service.
     *
     * @param authService the authentication service to use for securing requests
     */
    public ReviewApiClient(AuthenticationService authService) {
        super(authService);
    }

    /**
     * Retrieves a paginated list of reviews.
     *
     * @param page the page number to retrieve
     * @param size the maximum number of items per page
     * @return a paged response containing review results
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
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

    /**
     * Retrieves a paginated list of reviews that have been reported.
     *
     * @param page the page number to retrieve
     * @param size the maximum number of items per page
     * @return a paged response containing reported review results
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
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

    /**
     * Retrieves a paginated list of reports for a specific review.
     *
     * @param reviewId the unique identifier of the review
     * @param page the page number to retrieve
     * @param size the maximum number of items per page
     * @return a paged response containing review report results
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
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

    /**
     * Deletes a specific review by its identifier.
     *
     * @param id the unique identifier of the review to delete
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
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

    /**
     * Deletes a specific comment by its identifier.
     *
     * @param id the unique identifier of the comment to delete
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
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
