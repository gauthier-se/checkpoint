package com.checkpoint.api.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
@Table(name = "video_games")
public class VideoGame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationship: VideoGame can be played by multiple users (via UserGamePlay)
    @OneToMany(mappedBy = "videoGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserGamePlay> gamePlays = new HashSet<>();

    // Relationship: VideoGame can have multiple reviews
    @OneToMany(mappedBy = "videoGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    // Relationship: VideoGame can be available on multiple platforms (ManyToMany)
    @ManyToMany
    @JoinTable(
        name = "video_game_platforms",
        joinColumns = @JoinColumn(name = "video_game_id"),
        inverseJoinColumns = @JoinColumn(name = "platform_id")
    )
    private Set<Platform> platforms = new HashSet<>();

    // Relationship: VideoGame can have multiple genres (ManyToMany)
    @ManyToMany
    @JoinTable(
        name = "video_game_genres",
        joinColumns = @JoinColumn(name = "video_game_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    // Relationship: VideoGame can belong to 0 or 1 series
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private Series series;

    // Relationship: VideoGame can be created by multiple companies (ManyToMany)
    @ManyToMany
    @JoinTable(
        name = "video_game_companies",
        joinColumns = @JoinColumn(name = "video_game_id"),
        inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    private Set<Company> companies = new HashSet<>();

    // Relationship: VideoGame can have multiple pictures
    @OneToMany(mappedBy = "videoGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Picture> pictures = new HashSet<>();

    // Relationship: VideoGame can be a DLC of another game (self-referencing)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_game_id")
    private VideoGame parentGame;

    // Relationship: VideoGame can have multiple DLCs
    @OneToMany(mappedBy = "parentGame", cascade = CascadeType.ALL)
    private Set<VideoGame> dlcs = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public VideoGame() {}

    public VideoGame(String title, String description, LocalDate releaseDate) {
        this.title = title;
        this.description = description;
        this.releaseDate = releaseDate;
    }

    // Helper methods for managing platforms
    public void addPlatform(Platform platform) {
        this.platforms.add(platform);
        platform.getVideoGames().add(this);
    }

    public void removePlatform(Platform platform) {
        this.platforms.remove(platform);
        platform.getVideoGames().remove(this);
    }

    // Helper methods for managing genres
    public void addGenre(Genre genre) {
        this.genres.add(genre);
        genre.getVideoGames().add(this);
    }

    public void removeGenre(Genre genre) {
        this.genres.remove(genre);
        genre.getVideoGames().remove(this);
    }

    // Helper methods for managing companies
    public void addCompany(Company company) {
        this.companies.add(company);
        company.getVideoGames().add(this);
    }

    public void removeCompany(Company company) {
        this.companies.remove(company);
        company.getVideoGames().remove(this);
    }

    // Helper methods for managing DLCs
    public void addDlc(VideoGame dlc) {
        this.dlcs.add(dlc);
        dlc.setParentGame(this);
    }

    public void removeDlc(VideoGame dlc) {
        this.dlcs.remove(dlc);
        dlc.setParentGame(null);
    }

    // Getters and Setters
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

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
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

    public Set<UserGamePlay> getGamePlays() {
        return gamePlays;
    }

    public void setGamePlays(Set<UserGamePlay> gamePlays) {
        this.gamePlays = gamePlays;
    }

    public Set<Review> getReviews() {
        return reviews;
    }

    public void setReviews(Set<Review> reviews) {
        this.reviews = reviews;
    }

    public Set<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Set<Platform> platforms) {
        this.platforms = platforms;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public Set<Company> getCompanies() {
        return companies;
    }

    public void setCompanies(Set<Company> companies) {
        this.companies = companies;
    }

    public Set<Picture> getPictures() {
        return pictures;
    }

    public void setPictures(Set<Picture> pictures) {
        this.pictures = pictures;
    }

    public VideoGame getParentGame() {
        return parentGame;
    }

    public void setParentGame(VideoGame parentGame) {
        this.parentGame = parentGame;
    }

    public Set<VideoGame> getDlcs() {
        return dlcs;
    }

    public void setDlcs(Set<VideoGame> dlcs) {
        this.dlcs = dlcs;
    }
}
