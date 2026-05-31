package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing an external game search result from the API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalGameResult {
    private Long externalId;
    private String title;
    private Integer releaseYear;
    private String coverUrl;

    /**
     * Default constructor for ExternalGameResult.
     */
    public ExternalGameResult() {}

    /**
     * Gets the external ID of the game.
     *
     * @return the external ID
     */
    public Long getExternalId() { return externalId; }

    /**
     * Sets the external ID of the game.
     *
     * @param externalId the external ID to set
     */
    public void setExternalId(Long externalId) { this.externalId = externalId; }

    /**
     * Gets the title of the game.
     *
     * @return the title
     */
    public String getTitle() { return title; }

    /**
     * Sets the title of the game.
     *
     * @param title the title to set
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Gets the release year of the game.
     *
     * @return the release year
     */
    public Integer getReleaseYear() { return releaseYear; }

    /**
     * Sets the release year of the game.
     *
     * @param releaseYear the release year to set
     */
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    /**
     * Gets the cover URL of the game.
     *
     * @return the cover URL
     */
    public String getCoverUrl() { return coverUrl; }

    /**
     * Sets the cover URL of the game.
     *
     * @param coverUrl the cover URL to set
     */
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
}
