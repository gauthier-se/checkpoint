package com.seyzeriat.desktop.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO mirroring the analytics payload returned by {@code GET /api/admin/analytics}.
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

    public AnalyticsResult() {}

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getTotalGames() { return totalGames; }
    public void setTotalGames(long totalGames) { this.totalGames = totalGames; }

    public long getTotalReviews() { return totalReviews; }
    public void setTotalReviews(long totalReviews) { this.totalReviews = totalReviews; }

    public long getOpenReports() { return openReports; }
    public void setOpenReports(long openReports) { this.openReports = openReports; }

    public List<TopGame> getTopReviewedGames() { return topReviewedGames; }
    public void setTopReviewedGames(List<TopGame> topReviewedGames) { this.topReviewedGames = topReviewedGames; }

    public List<TopReviewer> getTopReviewers() { return topReviewers; }
    public void setTopReviewers(List<TopReviewer> topReviewers) { this.topReviewers = topReviewers; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopGame {
        private String id;
        private String title;
        private long reviewCount;

        public TopGame() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public long getReviewCount() { return reviewCount; }
        public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopReviewer {
        private String id;
        private String username;
        private long reviewCount;

        public TopReviewer() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public long getReviewCount() { return reviewCount; }
        public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
    }
}
