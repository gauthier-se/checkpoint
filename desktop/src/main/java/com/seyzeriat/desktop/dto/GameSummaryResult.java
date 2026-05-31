package com.seyzeriat.desktop.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for a game card returned by {@code GET /api/v1/games}.
 * Only the fields the manage-games table needs are kept.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameSummaryResult {
    private String id;
    private String title;
    private String coverUrl;
    private LocalDate releaseDate;

    /**
     * Default constructor for Jackson.
     */
    public GameSummaryResult() {}

    /**
     * Gets the game ID.
     * @return the game ID
     */
    public String getId() { return id; }

    /**
     * Sets the game ID.
     * @param id the game ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the title of the game.
     * @return the game title
     */
    public String getTitle() { return title; }

    /**
     * Sets the title of the game.
     * @param title the game title
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Gets the cover image URL.
     * @return the cover URL
     */
    public String getCoverUrl() { return coverUrl; }

    /**
     * Sets the cover image URL.
     * @param coverUrl the cover URL
     */
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    /**
     * Gets the release date of the game.
     * @return the release date
     */
    public LocalDate getReleaseDate() { return releaseDate; }

    /**
     * Sets the release date of the game.
     * @param releaseDate the release date
     */
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
}
