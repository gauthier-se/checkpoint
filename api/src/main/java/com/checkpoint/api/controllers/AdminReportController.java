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

import com.checkpoint.api.dto.admin.AdminReportDetailDto;
import com.checkpoint.api.dto.admin.AdminReportDto;
import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.services.AdminReportService;

/**
 * REST controller for admin report management operations.
 * All endpoints require the {@code ROLE_ADMIN} authority.
 */
@Tag(name = "Admin", description = "Admin: report queue")
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private static final Logger log = LoggerFactory.getLogger(AdminReportController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt,desc";

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    /**
     * Retrieves a paginated list of all reports, optionally filtered by type.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @param sort the sorting parameters
     * @param type optional filter: "review" or "comment"
     * @return the paginated reports
     */
    @GetMapping
    public ResponseEntity<PagedResponseDto<AdminReportDto>> getAllReports(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort,
            @RequestParam(required = false) String type) {

        log.info("Admin request: fetching all reports. Page: {}, Size: {}, Sort: {}, Type: {}", page, size, sort, type);

        int validatedSize = Math.min(Math.max(1, size), MAX_SIZE);
        int validatedPage = Math.max(0, page);

        Pageable pageable = createPageable(validatedPage, validatedSize, sort);
        PagedResponseDto<AdminReportDto> reports = adminReportService.getAllReports(pageable, type);

        return ResponseEntity.ok(reports);
    }

    /**
     * Retrieves the full details of a specific report.
     *
     * @param id the report ID
     * @return the report details
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminReportDetailDto> getReportById(@PathVariable UUID id) {
        log.info("Admin request: fetching report detail with id {}", id);

        AdminReportDetailDto report = adminReportService.getReportById(id);

        return ResponseEntity.ok(report);
    }

    /**
     * Dismisses (deletes) a report by its ID.
     *
     * @param id the ID of the report to dismiss
     * @return 204 No Content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> dismissReport(@PathVariable UUID id) {
        log.info("Admin request: dismissing report with id {}", id);

        adminReportService.dismissReport(id);

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
