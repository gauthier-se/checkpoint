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

public class ReportApiClient extends BaseApiClient implements ReportService {

    public ReportApiClient(AuthenticationService authService) {
        super(authService);
    }

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
