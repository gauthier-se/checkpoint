package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a news article returned by the admin API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsResult {
    private String id;
    private String title;
    private String description;
    private String picture;
    private String publishedAt;
    private String createdAt;
    private String updatedAt;
    private NewsAuthorResult author;
    private String source;
    private String externalUrl;
    private String feedName;
    private String videoGameId;

    /**
     * Default constructor for NewsResult.
     */
    public NewsResult() {}

    /**
     * Gets the ID of the news article.
     *
     * @return the ID
     */
    public String getId() { return id; }

    /**
     * Sets the ID of the news article.
     *
     * @param id the ID to set
     */
    public void setId(String id) { this.id = id; }

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

    /**
     * Gets the publish date of the news article.
     *
     * @return the published at date
     */
    public String getPublishedAt() { return publishedAt; }

    /**
     * Sets the publish date of the news article.
     *
     * @param publishedAt the published at date to set
     */
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    /**
     * Gets the creation date of the news article.
     *
     * @return the created at date
     */
    public String getCreatedAt() { return createdAt; }

    /**
     * Sets the creation date of the news article.
     *
     * @param createdAt the created at date to set
     */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * Gets the last update date of the news article.
     *
     * @return the updated at date
     */
    public String getUpdatedAt() { return updatedAt; }

    /**
     * Sets the last update date of the news article.
     *
     * @param updatedAt the updated at date to set
     */
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Gets the author of the news article.
     *
     * @return the author
     */
    public NewsAuthorResult getAuthor() { return author; }

    /**
     * Sets the author of the news article.
     *
     * @param author the author to set
     */
    public void setAuthor(NewsAuthorResult author) { this.author = author; }

    /**
     * Gets the source of the news article.
     *
     * @return the source
     */
    public String getSource() { return source; }

    /**
     * Sets the source of the news article.
     *
     * @param source the source to set
     */
    public void setSource(String source) { this.source = source; }

    /**
     * Gets the external URL of the news article.
     *
     * @return the external URL
     */
    public String getExternalUrl() { return externalUrl; }

    /**
     * Sets the external URL of the news article.
     *
     * @param externalUrl the external URL to set
     */
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    /**
     * Gets the feed name of the news article.
     *
     * @return the feed name
     */
    public String getFeedName() { return feedName; }

    /**
     * Sets the feed name of the news article.
     *
     * @param feedName the feed name to set
     */
    public void setFeedName(String feedName) { this.feedName = feedName; }

    /**
     * Gets the video game ID associated with the news article.
     *
     * @return the video game ID
     */
    public String getVideoGameId() { return videoGameId; }

    /**
     * Sets the video game ID associated with the news article.
     *
     * @param videoGameId the video game ID to set
     */
    public void setVideoGameId(String videoGameId) { this.videoGameId = videoGameId; }

    /**
     * Checks if the news article is published.
     *
     * @return true if published, false otherwise
     */
    public boolean isPublished() {
        return publishedAt != null && !publishedAt.isBlank();
    }
}
