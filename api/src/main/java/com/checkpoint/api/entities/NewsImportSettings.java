package com.checkpoint.api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Admin-editable knobs for the news importers, held in a single row.
 *
 * <p>These used to be compile-time constants and startup properties; they live in
 * the database so an admin can pause a source or cap the daily volume from the web
 * panel without a redeploy. Exactly one row is expected: it is created lazily with
 * the defaults below on first access, and
 * {@link com.checkpoint.api.services.NewsImportSettingsService} never creates a
 * second one.</p>
 *
 * <p>The import frequency is deliberately absent: the cron expressions on
 * {@link com.checkpoint.api.tasks.NewsImportTask} stay fixed, and pausing a source
 * turns its scheduled pass into a no-op rather than rescheduling it.</p>
 */
@Entity
@Table(name = "news_import_settings")
public class NewsImportSettings {

    /** Steam news items requested per game, matching the pre-settings constant. */
    public static final int DEFAULT_STEAM_NEWS_PER_GAME = 5;

    /** Daily ceiling applied on a fresh install: generous enough to be a safety net, not a throttle. */
    public static final int DEFAULT_MAX_ARTICLES_PER_DAY = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "steam_enabled", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean steamEnabled = true;

    @Column(name = "rss_enabled", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean rssEnabled = true;

    /**
     * Maximum articles all importers together may insert in one calendar day.
     * {@code null} means no ceiling at all.
     */
    @Column(name = "max_articles_per_day")
    private Integer maxArticlesPerDay = DEFAULT_MAX_ARTICLES_PER_DAY;

    @Column(name = "steam_news_per_game", nullable = false)
    private Integer steamNewsPerGame = DEFAULT_STEAM_NEWS_PER_GAME;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Username of the admin who last saved the form, for auditing. Null until the first edit. */
    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    @PreUpdate
    protected void onWrite() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public boolean isSteamEnabled() {
        return Boolean.TRUE.equals(steamEnabled);
    }

    public void setSteamEnabled(Boolean steamEnabled) {
        this.steamEnabled = steamEnabled;
    }

    public boolean isRssEnabled() {
        return Boolean.TRUE.equals(rssEnabled);
    }

    public void setRssEnabled(Boolean rssEnabled) {
        this.rssEnabled = rssEnabled;
    }

    public Integer getMaxArticlesPerDay() {
        return maxArticlesPerDay;
    }

    public void setMaxArticlesPerDay(Integer maxArticlesPerDay) {
        this.maxArticlesPerDay = maxArticlesPerDay;
    }

    public Integer getSteamNewsPerGame() {
        return steamNewsPerGame;
    }

    public void setSteamNewsPerGame(Integer steamNewsPerGame) {
        this.steamNewsPerGame = steamNewsPerGame;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
