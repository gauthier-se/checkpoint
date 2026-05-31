package com.seyzeriat.desktop.service;

import java.io.IOException;
import com.seyzeriat.desktop.dto.ReportResult;
import com.seyzeriat.desktop.dto.ReportDetailResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.exception.UnauthorizedException;

/**
 * Service interface for managing reports.
 * Provides operations to retrieve and manage reports within the application.
 */
public interface ReportService {

    /**
     * Retrieves a paginated list of reports.
     *
     * @param page the page number to retrieve (zero-based)
     * @param size the maximum number of items per page
     * @return a paged response containing report results
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    PagedResponse<ReportResult> getReports(int page, int size) throws IOException, InterruptedException, UnauthorizedException;
    /**
     * Dismisses a specific report by its identifier.
     *
     * @param id the unique identifier of the report to dismiss
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    void dismissReport(String id) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Retrieves detailed information about a specific report.
     *
     * @param id the unique identifier of the report
     * @return the detailed result for the specified report
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    ReportDetailResult getReportById(String id) throws IOException, InterruptedException, UnauthorizedException;
}
