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

    public NewsResult() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }

    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public NewsAuthorResult getAuthor() { return author; }
    public void setAuthor(NewsAuthorResult author) { this.author = author; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    public String getFeedName() { return feedName; }
    public void setFeedName(String feedName) { this.feedName = feedName; }

    public String getVideoGameId() { return videoGameId; }
    public void setVideoGameId(String videoGameId) { this.videoGameId = videoGameId; }

    public boolean isPublished() {
        return publishedAt != null && !publishedAt.isBlank();
    }
}
