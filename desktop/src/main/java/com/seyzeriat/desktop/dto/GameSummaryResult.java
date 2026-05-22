package com.seyzeriat.desktop.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for a game card returned by {@code GET /api/games}.
 * Only the fields the manage-games table needs are kept.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameSummaryResult {
    private String id;
    private String title;
    private String coverUrl;
    private LocalDate releaseDate;

    public GameSummaryResult() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
}
