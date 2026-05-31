package com.seyzeriat.desktop.service;

import java.io.IOException;
import com.seyzeriat.desktop.dto.AnalyticsResult;
import com.seyzeriat.desktop.exception.UnauthorizedException;

/**
 * Service interface for retrieving application analytics.
 */
public interface AnalyticsService {

    /**
     * Retrieves analytics information.
     *
     * @return the analytics result containing metrics
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    AnalyticsResult getAnalytics() throws IOException, InterruptedException, UnauthorizedException;
}
