package com.seyzeriat.desktop.service;

import java.io.IOException;
import com.seyzeriat.desktop.dto.AnalyticsResult;
import com.seyzeriat.desktop.exception.UnauthorizedException;

public interface AnalyticsService {
    AnalyticsResult getAnalytics() throws IOException, InterruptedException, UnauthorizedException;
}
