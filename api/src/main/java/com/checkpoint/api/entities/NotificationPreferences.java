package com.checkpoint.api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import com.checkpoint.api.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "follow_enabled", nullable = false)
    private Boolean followEnabled = true;

    @Column(name = "like_review_enabled", nullable = false)
    private Boolean likeReviewEnabled = true;

    @Column(name = "like_list_enabled", nullable = false)
    private Boolean likeListEnabled = true;

    @Column(name = "like_game_enabled", nullable = false)
    private Boolean likeGameEnabled = true;

    @Column(name = "comment_reply_enabled", nullable = false)
    private Boolean commentReplyEnabled = true;

    @Column(name = "level_up_enabled", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean levelUpEnabled = true;

    @Column(name = "badge_unlocked_enabled", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean badgeUnlockedEnabled = true;

    @Column(name = "mention_enabled", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean mentionEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public NotificationPreferences() {}

    public NotificationPreferences(User user) {
        this.user = user;
    }

    public boolean isEnabled(NotificationType type) {
        return switch (type) {
            case FOLLOW -> Boolean.TRUE.equals(followEnabled);
            case LIKE_REVIEW -> Boolean.TRUE.equals(likeReviewEnabled);
            case LIKE_LIST -> Boolean.TRUE.equals(likeListEnabled);
            case LIKE_GAME -> Boolean.TRUE.equals(likeGameEnabled);
            case COMMENT_REPLY -> Boolean.TRUE.equals(commentReplyEnabled);
            case LEVEL_UP -> Boolean.TRUE.equals(levelUpEnabled);
            case BADGE_UNLOCKED -> Boolean.TRUE.equals(badgeUnlockedEnabled);
            case MENTION -> Boolean.TRUE.equals(mentionEnabled);
        };
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getFollowEnabled() {
        return followEnabled;
    }

    public void setFollowEnabled(Boolean followEnabled) {
        this.followEnabled = followEnabled;
    }

    public Boolean getLikeReviewEnabled() {
        return likeReviewEnabled;
    }

    public void setLikeReviewEnabled(Boolean likeReviewEnabled) {
        this.likeReviewEnabled = likeReviewEnabled;
    }

    public Boolean getLikeListEnabled() {
        return likeListEnabled;
    }

    public void setLikeListEnabled(Boolean likeListEnabled) {
        this.likeListEnabled = likeListEnabled;
    }

    public Boolean getLikeGameEnabled() {
        return likeGameEnabled;
    }

    public void setLikeGameEnabled(Boolean likeGameEnabled) {
        this.likeGameEnabled = likeGameEnabled;
    }

    public Boolean getCommentReplyEnabled() {
        return commentReplyEnabled;
    }

    public void setCommentReplyEnabled(Boolean commentReplyEnabled) {
        this.commentReplyEnabled = commentReplyEnabled;
    }

    public Boolean getLevelUpEnabled() {
        return levelUpEnabled;
    }

    public void setLevelUpEnabled(Boolean levelUpEnabled) {
        this.levelUpEnabled = levelUpEnabled;
    }

    public Boolean getBadgeUnlockedEnabled() {
        return badgeUnlockedEnabled;
    }

    public void setBadgeUnlockedEnabled(Boolean badgeUnlockedEnabled) {
        this.badgeUnlockedEnabled = badgeUnlockedEnabled;
    }

    public Boolean getMentionEnabled() {
        return mentionEnabled;
    }

    public void setMentionEnabled(Boolean mentionEnabled) {
        this.mentionEnabled = mentionEnabled;
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
}
