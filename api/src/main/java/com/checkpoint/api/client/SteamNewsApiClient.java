package com.checkpoint.api.client;

import java.util.List;

import com.checkpoint.api.dto.steam.SteamNewsResponseDto;

/**
 * Client for Steam's public {@code ISteamNews/GetNewsForApp} endpoint.
 *
 * <p>Unlike most Steam Web API endpoints, GetNewsForApp does not require an API key.
 * Implementations rate-limit to 1 request per second to stay polite.</p>
 */
public interface SteamNewsApiClient {

    /**
     * Fetches the latest news items for a Steam application.
     *
     * @param steamAppId the Steam application ID
     * @param count      the maximum number of items to return (Steam caps this at 100)
     * @return the news items in publication order (newest first), or an empty list if
     *         the app has no news. Never returns {@code null}.
     */
    List<SteamNewsResponseDto.NewsItem> fetchNewsForApp(long steamAppId, int count);
}
