package com.checkpoint.api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Like entity that can be associated with a VideoGame, Review, or GameList.
 * A user can like multiple items (polymorphic relationship).
 */
@Entity
@Table(name = "likes")
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationship: Like belongs to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Relationship: Like can be for a video game (nullable - polymorphic)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_game_id")
    private VideoGame videoGame;

    // Relationship: Like can be for a review (nullable - polymorphic)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    // Relationship: Like can be for a list (nullable - polymorphic)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    private GameList gameList;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Like() {}

    public Like(User user, VideoGame videoGame) {
        this.user = user;
        this.videoGame = videoGame;
    }

    // Factory methods for clarity
    public static Like forVideoGame(User user, VideoGame videoGame) {
        Like like = new Like();
        like.setUser(user);
        like.setVideoGame(videoGame);
        return like;
    }

    public static Like forReview(User user, Review review) {
        Like like = new Like();
        like.setUser(user);
        like.setReview(review);
        return like;
    }

    public static Like forGameList(User user, GameList gameList) {
        Like like = new Like();
        like.setUser(user);
        like.setGameList(gameList);
        return like;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public VideoGame getVideoGame() {
        return videoGame;
    }

    public void setVideoGame(VideoGame videoGame) {
        this.videoGame = videoGame;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public GameList getGameList() {
        return gameList;
    }

    public void setGameList(GameList gameList) {
        this.gameList = gameList;
    }
}
