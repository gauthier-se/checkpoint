package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a detailed user profile returned by the admin API.
 * Maps the JSON response from {@code GET /api/v1/admin/users/{id}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetailResult {

    private String id;
    private String username;
    private String email;
    private String bio;
    private String picture;
    private boolean isPrivate;
    private boolean banned;
    private int xpPoint;
    private int level;
    private String createdAt;
    private long reviewCount;
    private long reportCount;

    /**
     * Default constructor for Jackson.
     */
    public UserDetailResult() {}

    /**
     * Gets the user ID.
     * @return the user ID
     */
    public String getId() { return id; }
    
    /**
     * Sets the user ID.
     * @param id the user ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the username.
     * @return the username
     */
    public String getUsername() { return username; }
    
    /**
     * Sets the username.
     * @param username the username
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Gets the user email.
     * @return the email
     */
    public String getEmail() { return email; }
    
    /**
     * Sets the user email.
     * @param email the email
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Gets the user biography.
     * @return the bio
     */
    public String getBio() { return bio; }
    
    /**
     * Sets the user biography.
     * @param bio the bio
     */
    public void setBio(String bio) { this.bio = bio; }

    /**
     * Gets the user profile picture URL.
     * @return the picture URL
     */
    public String getPicture() { return picture; }
    
    /**
     * Sets the user profile picture URL.
     * @param picture the picture URL
     */
    public void setPicture(String picture) { this.picture = picture; }

    /**
     * Checks if the user profile is private.
     * @return true if the profile is private, false otherwise
     */
    public boolean isPrivate() { return isPrivate; }
    
    /**
     * Sets whether the user profile is private.
     * @param isPrivate true if private, false otherwise
     */
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    /**
     * Checks if the user is banned.
     * @return true if the user is banned, false otherwise
     */
    public boolean isBanned() { return banned; }
    
    /**
     * Sets whether the user is banned.
     * @param banned true if banned, false otherwise
     */
    public void setBanned(boolean banned) { this.banned = banned; }

    /**
     * Gets the user's experience points.
     * @return XP points
     */
    public int getXpPoint() { return xpPoint; }
    
    /**
     * Sets the user's experience points.
     * @param xpPoint XP points
     */
    public void setXpPoint(int xpPoint) { this.xpPoint = xpPoint; }

    /**
     * Gets the user's level.
     * @return user level
     */
    public int getLevel() { return level; }
    
    /**
     * Sets the user's level.
     * @param level user level
     */
    public void setLevel(int level) { this.level = level; }

    /**
     * Gets the timestamp when the user account was created.
     * @return creation timestamp
     */
    public String getCreatedAt() { return createdAt; }
    
    /**
     * Sets the timestamp when the user account was created.
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * Gets the user's total review count.
     * @return review count
     */
    public long getReviewCount() { return reviewCount; }
    
    /**
     * Sets the user's total review count.
     * @param reviewCount review count
     */
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }

    /**
     * Gets the user's total report count.
     * @return report count
     */
    public long getReportCount() { return reportCount; }
    
    /**
     * Sets the user's total report count.
     * @param reportCount report count
     */
    public void setReportCount(long reportCount) { this.reportCount = reportCount; }
}
