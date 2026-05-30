package com.seyzeriat.desktop.service;

import java.io.IOException;
import com.seyzeriat.desktop.dto.ReportResult;
import com.seyzeriat.desktop.dto.ReportDetailResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.exception.UnauthorizedException;

public interface ReportService {
    PagedResponse<ReportResult> getReports(int page, int size) throws IOException, InterruptedException, UnauthorizedException;
    void dismissReport(String id) throws IOException, InterruptedException, UnauthorizedException;
    ReportDetailResult getReportById(String id) throws IOException, InterruptedException, UnauthorizedException;
}
