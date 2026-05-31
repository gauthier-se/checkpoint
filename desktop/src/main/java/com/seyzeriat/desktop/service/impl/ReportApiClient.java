package com.seyzeriat.desktop.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.dto.ReportDetailResult;
import com.seyzeriat.desktop.dto.ReportResult;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.ReportService;

/**
 * API client implementation for {@link ReportService}.
 * Handles HTTP communication with the backend to manage reports.
 */
public class ReportApiClient extends BaseApiClient implements ReportService {

    /**
     * Constructs a new ReportApiClient with the specified authentication service.
     *
     * @param authService the authentication service to use for securing requests
     */
    public ReportApiClient(AuthenticationService authService) {
        super(authService);
    }

    /**
     * Retrieves a paginated list of reports.
     *
     * @param page the page number to retrieve
     * @param size the maximum number of items per page
     * @return a paged response containing report results
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public PagedResponse<ReportResult> getReports(int page, int size) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/reports?page=" + page + "&size=" + size;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch reports with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<PagedResponse<ReportResult>>() {});
    }

    /**
     * Dismisses a specific report by its identifier.
     *
     * @param id the unique identifier of the report to dismiss
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public void dismissReport(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/reports/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to dismiss report with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Retrieves detailed information about a specific report.
     *
     * @param id the unique identifier of the report
     * @return the detailed result for the specified report
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public ReportDetailResult getReportById(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/reports/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch report detail with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), ReportDetailResult.class);
    }
}
