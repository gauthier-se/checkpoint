package com.checkpoint.api.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Formula;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "lists")
public class GameList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Computed field: count of video games in this list
    @Formula("(SELECT COUNT(*) FROM list_video_games lvg WHERE lvg.list_id = id)")
    private Integer videoGamesCount;

    // Relationship: List is created by one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Relationship: List can contain multiple video games (ManyToMany)
    @ManyToMany
    @JoinTable(
        name = "list_video_games",
        joinColumns = @JoinColumn(name = "list_id"),
        inverseJoinColumns = @JoinColumn(name = "video_game_id")
    )
    private Set<VideoGame> videoGames = new HashSet<>();

    // Relationship: List can have multiple comments
    @OneToMany(mappedBy = "gameList", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comments = new HashSet<>();

    // Relationship: List can have multiple likes
    @OneToMany(mappedBy = "gameList", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Like> likes = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public GameList() {}

    public GameList(String title, User user) {
        this.title = title;
        this.user = user;
    }

    // Helper methods for managing video games
    public void addVideoGame(VideoGame videoGame) {
        this.videoGames.add(videoGame);
        videoGame.getGameLists().add(this);
    }

    public void removeVideoGame(VideoGame videoGame) {
        this.videoGames.remove(videoGame);
        videoGame.getGameLists().remove(this);
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

    public Set<VideoGame> getVideoGames() {
        return videoGames;
    }

    public void setVideoGames(Set<VideoGame> videoGames) {
        this.videoGames = videoGames;
    }

    public Set<Comment> getComments() {
        return comments;
    }

    public void setComments(Set<Comment> comments) {
        this.comments = comments;
    }

    public Set<Like> getLikes() {
        return likes;
    }

    public void setLikes(Set<Like> likes) {
        this.likes = likes;
    }

    public Integer getVideoGamesCount() {
        return videoGamesCount != null ? videoGamesCount : 0;
    }
}
