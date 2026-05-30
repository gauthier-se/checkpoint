package com.seyzeriat.desktop.service;

import java.io.IOException;
import com.seyzeriat.desktop.dto.ReviewResult;
import com.seyzeriat.desktop.dto.ReviewReportResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.exception.UnauthorizedException;

public interface ReviewService {
    PagedResponse<ReviewResult> getReviews(int page, int size) throws IOException, InterruptedException, UnauthorizedException;
    PagedResponse<ReviewResult> getReportedReviews(int page, int size) throws IOException, InterruptedException, UnauthorizedException;
    PagedResponse<ReviewReportResult> getReviewReports(String reviewId, int page, int size) throws IOException, InterruptedException, UnauthorizedException;
    void deleteReview(String id) throws IOException, InterruptedException, UnauthorizedException;
    void deleteComment(String id) throws IOException, InterruptedException, UnauthorizedException;
}
