package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a user review returned by the admin API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewResult {
    private String id;
    private String content;
    private Boolean haveSpoilers;
    private String authorId;
    private String authorUsername;
    private String gameTitle;
    private String createdAt;
    private long reportCount;

    /**
     * Default constructor for ReviewResult.
     */
    public ReviewResult() {}

    /**
     * Gets the ID of the review.
     *
     * @return the review ID
     */
    public String getId() { return id; }

    /**
     * Sets the ID of the review.
     *
     * @param id the review ID to set
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the content of the review.
     *
     * @return the review content
     */
    public String getContent() { return content; }

    /**
     * Sets the content of the review.
     *
     * @param content the review content to set
     */
    public void setContent(String content) { this.content = content; }

    /**
     * Indicates whether the review contains spoilers.
     *
     * @return true if it has spoilers, false otherwise
     */
    public Boolean getHaveSpoilers() { return haveSpoilers; }

    /**
     * Sets whether the review contains spoilers.
     *
     * @param haveSpoilers spoiler flag to set
     */
    public void setHaveSpoilers(Boolean haveSpoilers) { this.haveSpoilers = haveSpoilers; }

    /**
     * Gets the ID of the author.
     *
     * @return the author ID
     */
    public String getAuthorId() { return authorId; }

    /**
     * Sets the ID of the author.
     *
     * @param authorId the author ID to set
     */
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    /**
     * Gets the username of the author.
     *
     * @return the author username
     */
    public String getAuthorUsername() { return authorUsername; }

    /**
     * Sets the username of the author.
     *
     * @param authorUsername the author username to set
     */
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    /**
     * Gets the title of the game being reviewed.
     *
     * @return the game title
     */
    public String getGameTitle() { return gameTitle; }

    /**
     * Sets the title of the game being reviewed.
     *
     * @param gameTitle the game title to set
     */
    public void setGameTitle(String gameTitle) { this.gameTitle = gameTitle; }

    /**
     * Gets the creation date of the review.
     *
     * @return the created at date
     */
    public String getCreatedAt() { return createdAt; }

    /**
     * Sets the creation date of the review.
     *
     * @param createdAt the created at date to set
     */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * Gets the number of times this review has been reported.
     *
     * @return the report count
     */
    public long getReportCount() { return reportCount; }

    /**
     * Sets the number of times this review has been reported.
     *
     * @param reportCount the report count to set
     */
    public void setReportCount(long reportCount) { this.reportCount = reportCount; }
}
