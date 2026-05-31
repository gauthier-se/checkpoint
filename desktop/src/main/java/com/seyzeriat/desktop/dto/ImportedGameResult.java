package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing the result of a game import from the API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportedGameResult {
    private String id;
    private String title;
    private String description;
    private String coverUrl;

    /**
     * Default constructor for ImportedGameResult.
     */
    public ImportedGameResult() {}

    /**
     * Gets the ID of the imported game.
     *
     * @return the game ID
     */
    public String getId() { return id; }

    /**
     * Sets the ID of the imported game.
     *
     * @param id the game ID to set
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the title of the imported game.
     *
     * @return the game title
     */
    public String getTitle() { return title; }

    /**
     * Sets the title of the imported game.
     *
     * @param title the game title to set
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Gets the description of the imported game.
     *
     * @return the game description
     */
    public String getDescription() { return description; }

    /**
     * Sets the description of the imported game.
     *
     * @param description the game description to set
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Gets the cover URL of the imported game.
     *
     * @return the cover URL
     */
    public String getCoverUrl() { return coverUrl; }

    /**
     * Sets the cover URL of the imported game.
     *
     * @param coverUrl the cover URL to set
     */
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
}
