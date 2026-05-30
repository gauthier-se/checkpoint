package com.checkpoint.api.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import com.checkpoint.api.dto.admin.AdminReportedReviewDto;
import com.checkpoint.api.dto.admin.AdminReviewDto;
import com.checkpoint.api.dto.admin.AdminReviewReportDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.services.AdminReviewService;

/**
 * REST controller for admin review management operations.
 * All endpoints require the {@code ROLE_ADMIN} authority.
 */
@Tag(name = "Admin", description = "Admin: review moderation")
@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private static final Logger log = LoggerFactory.getLogger(AdminReviewController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt,desc";

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    /**
     * Retrieves a paginated list of all reviews.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @param sort the sorting parameters
     * @return the paginated reviews
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<AdminReviewDto>> getAllReviews(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("Admin request: fetching all reviews. Page: {}, Size: {}, Sort: {}", page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        PagedResponseDto<AdminReviewDto> reviews = adminReviewService.getAllReviews(pageable);

        return ResponseEntity.ok(reviews);
    }

    /**
     * Retrieves a paginated list of reported reviews (reviews with at least one report).
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @param sort the sorting parameters
     * @return the paginated reported reviews
     */
    @GetMapping("/reported")
    public ResponseEntity<PagedResponseDto<AdminReportedReviewDto>> getReportedReviews(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("Admin request: fetching reported reviews. Page: {}, Size: {}, Sort: {}", page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        PagedResponseDto<AdminReportedReviewDto> reviews = adminReviewService.getReportedReviews(pageable);

        return ResponseEntity.ok(reviews);
    }

    /**
     * Retrieves a paginated list of reports filed against a specific review.
     *
     * @param id   the ID of the review whose reports are fetched
     * @param page the page number (0-based)
     * @param size the page size
     * @param sort the sorting parameters
     * @return the paginated reports targeting the review
     */
    @GetMapping("/{id}/reports")
    public ResponseEntity<PagedResponseDto<AdminReviewReportDto>> getReviewReports(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        log.info("Admin request: fetching reports for review {}. Page: {}, Size: {}, Sort: {}", id, page, size, sort);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        PagedResponseDto<AdminReviewReportDto> reports = adminReviewService.getReviewReports(id, pageable);

        return ResponseEntity.ok(reports);
    }

    /**
     * Deletes a review by its ID.
     *
     * @param id the ID of the review to delete
     * @return 204 No Content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        log.info("Admin request: deleting review with id {}", id);

        adminReviewService.deleteReview(id);

        return ResponseEntity.noContent().build();
    }

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

    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "createdAt";
        };
    }
}
