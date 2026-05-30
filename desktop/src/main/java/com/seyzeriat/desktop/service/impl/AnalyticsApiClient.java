package com.seyzeriat.desktop.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.seyzeriat.desktop.dto.AnalyticsResult;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.service.AnalyticsService;
import com.seyzeriat.desktop.service.AuthenticationService;

public class AnalyticsApiClient extends BaseApiClient implements AnalyticsService {

    public AnalyticsApiClient(AuthenticationService authService) {
        super(authService);
    }

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
