package com.checkpoint.api.dto.steam;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level wrapper for the {@code ISteamNews/GetNewsForApp} response.
 *
 * <p>The Steam endpoint returns:
 * <pre>{ "appnews": { "appid": 730, "newsitems": [ ... ] } }</pre>
 *
 * <p>Only the fields the news importer needs are mapped; the rest are ignored.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamNewsResponseDto(
        @JsonProperty("appnews") AppNews appnews
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AppNews(
            @JsonProperty("appid") Long appid,
            @JsonProperty("newsitems") List<NewsItem> newsitems
    ) {}

    /**
     * A single Steam news item. {@code date} is a UNIX epoch in seconds.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NewsItem(
            @JsonProperty("gid") String gid,
            @JsonProperty("title") String title,
            @JsonProperty("url") String url,
            @JsonProperty("author") String author,
            @JsonProperty("contents") String contents,
            @JsonProperty("feedlabel") String feedlabel,
            @JsonProperty("date") Long date,
            @JsonProperty("feedname") String feedname,
            @JsonProperty("appid") Long appid
    ) {}
}
