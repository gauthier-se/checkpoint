package com.seyzeriat.desktop.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO mirroring the analytics payload returned by {@code GET /api/v1/admin/analytics}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyticsResult {

    private long totalUsers;
    private long activeUsers;
    private long totalGames;
    private long totalReviews;
    private long openReports;
    private List<TopGame> topReviewedGames;
    private List<TopReviewer> topReviewers;

    /**
     * Default constructor for Jackson.
     */
    public AnalyticsResult() {}

    /**
     * Gets the total number of users.
     * @return total users
     */
    public long getTotalUsers() { return totalUsers; }
    
    /**
     * Sets the total number of users.
     * @param totalUsers total users
     */
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    /**
     * Gets the number of active users.
     * @return active users
     */
    public long getActiveUsers() { return activeUsers; }
    
    /**
     * Sets the number of active users.
     * @param activeUsers active users
     */
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    /**
     * Gets the total number of games.
     * @return total games
     */
    public long getTotalGames() { return totalGames; }
    
    /**
     * Sets the total number of games.
     * @param totalGames total games
     */
    public void setTotalGames(long totalGames) { this.totalGames = totalGames; }

    /**
     * Gets the total number of reviews.
     * @return total reviews
     */
    public long getTotalReviews() { return totalReviews; }
    
    /**
     * Sets the total number of reviews.
     * @param totalReviews total reviews
     */
    public void setTotalReviews(long totalReviews) { this.totalReviews = totalReviews; }

    /**
     * Gets the number of open reports.
     * @return open reports
     */
    public long getOpenReports() { return openReports; }
    
    /**
     * Sets the number of open reports.
     * @param openReports open reports
     */
    public void setOpenReports(long openReports) { this.openReports = openReports; }

    /**
     * Gets the list of top reviewed games.
     * @return top reviewed games
     */
    public List<TopGame> getTopReviewedGames() { return topReviewedGames; }
    
    /**
     * Sets the list of top reviewed games.
     * @param topReviewedGames top reviewed games
     */
    public void setTopReviewedGames(List<TopGame> topReviewedGames) { this.topReviewedGames = topReviewedGames; }

    /**
     * Gets the list of top reviewers.
     * @return top reviewers
     */
    public List<TopReviewer> getTopReviewers() { return topReviewers; }
    
    /**
     * Sets the list of top reviewers.
     * @param topReviewers top reviewers
     */
    public void setTopReviewers(List<TopReviewer> topReviewers) { this.topReviewers = topReviewers; }

    /**
     * DTO representing a top reviewed game in analytics.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopGame {
        private String id;
        private String title;
        private long reviewCount;

        /**
         * Default constructor for Jackson.
         */
        public TopGame() {}

        /**
         * Gets the game ID.
         * @return game ID
         */
        public String getId() { return id; }
        
        /**
         * Sets the game ID.
         * @param id game ID
         */
        public void setId(String id) { this.id = id; }

        /**
         * Gets the game title.
         * @return game title
         */
        public String getTitle() { return title; }
        
        /**
         * Sets the game title.
         * @param title game title
         */
        public void setTitle(String title) { this.title = title; }

        /**
         * Gets the review count for the game.
         * @return review count
         */
        public long getReviewCount() { return reviewCount; }
        
        /**
         * Sets the review count for the game.
         * @param reviewCount review count
         */
        public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
    }

    /**
     * DTO representing a top reviewer in analytics.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopReviewer {
        private String id;
        private String username;
        private long reviewCount;

        /**
         * Default constructor for Jackson.
         */
        public TopReviewer() {}

        /**
         * Gets the reviewer ID.
         * @return reviewer ID
         */
        public String getId() { return id; }
        
        /**
         * Sets the reviewer ID.
         * @param id reviewer ID
         */
        public void setId(String id) { this.id = id; }

        /**
         * Gets the reviewer's username.
         * @return username
         */
        public String getUsername() { return username; }
        
        /**
         * Sets the reviewer's username.
         * @param username username
         */
        public void setUsername(String username) { this.username = username; }

        /**
         * Gets the user's review count.
         * @return review count
         */
        public long getReviewCount() { return reviewCount; }
        
        /**
         * Sets the user's review count.
         * @param reviewCount review count
         */
        public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
    }
}
