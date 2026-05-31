package com.seyzeriat.desktop.dto;

/**
 * Payload sent to the admin API when creating or updating a news article.
 *
 * <p>Mirrors the API's {@code NewsRequestDto}: title, description (markdown
 * content) and picture (cover image URL). The author is derived from the JWT
 * server-side and is therefore not part of this payload.</p>
 */
public class NewsRequestPayload {
    private String title;
    private String description;
    private String picture;

    /**
     * Default constructor for NewsRequestPayload.
     */
    public NewsRequestPayload() {}

    /**
     * Constructs a new NewsRequestPayload with the given title, description, and picture.
     *
     * @param title the title of the news article
     * @param description the description or markdown content of the news article
     * @param picture the cover image URL
     */
    public NewsRequestPayload(String title, String description, String picture) {
        this.title = title;
        this.description = description;
        this.picture = picture;
    }

    /**
     * Gets the title of the news article.
     *
     * @return the title
     */
    public String getTitle() { return title; }

    /**
     * Sets the title of the news article.
     *
     * @param title the title to set
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Gets the description of the news article.
     *
     * @return the description
     */
    public String getDescription() { return description; }

    /**
     * Sets the description of the news article.
     *
     * @param description the description to set
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Gets the picture URL of the news article.
     *
     * @return the picture URL
     */
    public String getPicture() { return picture; }

    /**
     * Sets the picture URL of the news article.
     *
     * @param picture the picture URL to set
     */
    public void setPicture(String picture) { this.picture = picture; }
}
