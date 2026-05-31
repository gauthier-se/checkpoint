package com.seyzeriat.desktop.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.seyzeriat.desktop.dto.AnalyticsResult;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.service.AnalyticsService;
import com.seyzeriat.desktop.service.AuthenticationService;

/**
 * API client implementation for {@link AnalyticsService}.
 * Handles HTTP communication with the backend to retrieve analytics.
 */
public class AnalyticsApiClient extends BaseApiClient implements AnalyticsService {

    /**
     * Constructs a new AnalyticsApiClient with the specified authentication service.
     *
     * @param authService the authentication service to use for securing requests
     */
    public AnalyticsApiClient(AuthenticationService authService) {
        super(authService);
    }

    /**
     * Retrieves analytics information.
     *
     * @return the analytics result containing metrics
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public AnalyticsResult getAnalytics() throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/analytics";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch analytics with status "
                    + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), AnalyticsResult.class);
    }
}
