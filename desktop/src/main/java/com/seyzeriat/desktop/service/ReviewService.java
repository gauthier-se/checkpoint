package com.seyzeriat.desktop.service;

import java.io.IOException;
import com.seyzeriat.desktop.dto.ReviewResult;
import com.seyzeriat.desktop.dto.ReviewReportResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.exception.UnauthorizedException;

/**
 * Service interface for managing reviews and related reports.
 * Provides operations to retrieve, delete, and manage user reviews and comments.
 */
public interface ReviewService {

    /**
     * Retrieves a paginated list of reviews.
     *
     * @param page the page number to retrieve (zero-based)
     * @param size the maximum number of items per page
     * @return a paged response containing review results
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    PagedResponse<ReviewResult> getReviews(int page, int size) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Retrieves a paginated list of reviews that have been reported.
     *
     * @param page the page number to retrieve (zero-based)
     * @param size the maximum number of items per page
     * @return a paged response containing reported review results
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    PagedResponse<ReviewResult> getReportedReviews(int page, int size) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Retrieves a paginated list of reports for a specific review.
     *
     * @param reviewId the unique identifier of the review
     * @param page the page number to retrieve (zero-based)
     * @param size the maximum number of items per page
     * @return a paged response containing review report results
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    PagedResponse<ReviewReportResult> getReviewReports(String reviewId, int page, int size) throws IOException, InterruptedException, UnauthorizedException;
    /**
     * Deletes a specific review by its identifier.
     *
     * @param id the unique identifier of the review to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    void deleteReview(String id) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Deletes a specific comment by its identifier.
     *
     * @param id the unique identifier of the comment to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    void deleteComment(String id) throws IOException, InterruptedException, UnauthorizedException;
}
