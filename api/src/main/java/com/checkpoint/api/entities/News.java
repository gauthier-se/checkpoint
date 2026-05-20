package com.checkpoint.api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "news",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_news_source_external_id",
                columnNames = {"source", "external_id"}
        ),
        indexes = {
                @Index(name = "idx_news_external_id", columnList = "external_id")
        }
)
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private String picture;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Author is nullable: imported news (STEAM/RSS) have no human author —
    // attribution lives in {@code feedName} and {@code externalUrl} instead.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "source",
            nullable = false,
            length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'MANUAL'"
    )
    private NewsSource source = NewsSource.MANUAL;

    // Feed-item GUID (RSS) or Steam {@code gid}. Null for MANUAL entries.
    @Column(name = "external_id")
    private String externalId;

    // Link to the original article. Null for MANUAL entries.
    @Column(name = "external_url", length = 2048)
    private String externalUrl;

    // Human-readable feed name ("Steam Community", "IGN"). Null for MANUAL entries.
    @Column(name = "feed_name")
    private String feedName;

    // Set only for STEAM per-game news.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_game_id")
    private VideoGame videoGame;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public News() {}

    public News(String title, String description, User author) {
        this.title = title;
        this.description = description;
        this.author = author;
    }

    /**
     * Constructor used by the news import task for STEAM/RSS entries.
     */
    public News(String title,
                String description,
                NewsSource source,
                String externalId,
                String externalUrl,
                String feedName,
                VideoGame videoGame) {
        this.title = title;
        this.description = description;
        this.source = source;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.feedName = feedName;
        this.videoGame = videoGame;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public NewsSource getSource() {
        return source;
    }

    public void setSource(NewsSource source) {
        this.source = source;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public VideoGame getVideoGame() {
        return videoGame;
    }

    public void setVideoGame(VideoGame videoGame) {
        this.videoGame = videoGame;
    }
}
