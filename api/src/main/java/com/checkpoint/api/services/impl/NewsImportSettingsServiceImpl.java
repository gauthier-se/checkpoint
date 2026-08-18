package com.checkpoint.api.services.impl;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.NewsImportSettingsDto;
import com.checkpoint.api.dto.catalog.NewsImportSettingsRequestDto;
import com.checkpoint.api.entities.NewsImportSettings;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.repositories.NewsImportSettingsRepository;
import com.checkpoint.api.repositories.NewsRepository;
import com.checkpoint.api.services.NewsImportSettingsService;

/**
 * Implementation of {@link NewsImportSettingsService}.
 *
 * <p>The daily ceiling is not a stored counter: it is derived on every read from
 * {@code news.created_at}, so a restart, a manual deletion or a clock crossing
 * midnight all resolve themselves without a reset job.</p>
 */
@Service
@Transactional
public class NewsImportSettingsServiceImpl implements NewsImportSettingsService {

    private static final Logger log = LoggerFactory.getLogger(NewsImportSettingsServiceImpl.class);

    /** Sources the ceiling governs: MANUAL articles are written by hand, not imported. */
    private static final Set<NewsSource> IMPORTED_SOURCES = EnumSet.of(NewsSource.STEAM, NewsSource.RSS);

    /** Upper bounds, wide enough not to get in the way but low enough to catch a fat finger. */
    private static final int MAX_ARTICLES_PER_DAY_LIMIT = 10_000;
    private static final int MAX_STEAM_NEWS_PER_GAME = 50;

    private final NewsImportSettingsRepository settingsRepository;
    private final NewsRepository newsRepository;

    public NewsImportSettingsServiceImpl(NewsImportSettingsRepository settingsRepository,
                                         NewsRepository newsRepository) {
        this.settingsRepository = settingsRepository;
        this.newsRepository = newsRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public NewsImportSettingsDto get() {
        return toDto(current());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NewsImportSettingsDto update(String adminUsername, NewsImportSettingsRequestDto request) {
        NewsImportSettings settings = current();

        if (request.steamEnabled() != null) {
            settings.setSteamEnabled(request.steamEnabled());
        }
        if (request.rssEnabled() != null) {
            settings.setRssEnabled(request.rssEnabled());
        }
        if (Boolean.TRUE.equals(request.unlimited())) {
            settings.setMaxArticlesPerDay(null);
        } else if (request.maxArticlesPerDay() != null) {
            settings.setMaxArticlesPerDay(
                    validateRange("maxArticlesPerDay", request.maxArticlesPerDay(), 0, MAX_ARTICLES_PER_DAY_LIMIT));
        }
        if (request.steamNewsPerGame() != null) {
            settings.setSteamNewsPerGame(
                    validateRange("steamNewsPerGame", request.steamNewsPerGame(), 1, MAX_STEAM_NEWS_PER_GAME));
        }
        settings.setUpdatedBy(adminUsername);

        NewsImportSettings saved = settingsRepository.save(settings);
        log.info("News import settings updated by {}: steam={}, rss={}, maxPerDay={}, steamPerGame={}",
                adminUsername, saved.isSteamEnabled(), saved.isRssEnabled(),
                saved.getMaxArticlesPerDay(), saved.getSteamNewsPerGame());
        return toDto(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isScheduledPassEnabled(NewsSource source) {
        NewsImportSettings settings = current();
        return switch (source) {
            case STEAM -> settings.isSteamEnabled();
            case RSS -> settings.isRssEnabled();
            // No scheduled pass imports MANUAL news, so nothing can enable one.
            case MANUAL -> false;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public int remainingDailyBudget() {
        Integer ceiling = current().getMaxArticlesPerDay();
        if (ceiling == null) {
            return Integer.MAX_VALUE;
        }
        long used = importedToday();
        return (int) Math.max(0, ceiling - used);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public int steamNewsPerGame() {
        return current().getSteamNewsPerGame();
    }

    /**
     * Returns the stored settings, or a transient instance carrying the defaults
     * when no admin has ever saved the form. Reads never insert: an untouched
     * installation behaves exactly as the defaults describe, and the row appears
     * the first time somebody actually changes something: {@link #update} saves
     * whatever this returns, transient or not.
     */
    private NewsImportSettings current() {
        return settingsRepository.findFirstByOrderByIdAsc().orElseGet(NewsImportSettings::new);
    }

    private long importedToday() {
        return newsRepository.countBySourceInAndCreatedAtGreaterThanEqual(
                IMPORTED_SOURCES, LocalDate.now().atStartOfDay());
    }

    private int validateRange(String field, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "%s must be between %d and %d, got %d".formatted(field, min, max, value));
        }
        return value;
    }

    private NewsImportSettingsDto toDto(NewsImportSettings settings) {
        long importedToday = importedToday();
        Integer ceiling = settings.getMaxArticlesPerDay();
        Integer remaining = ceiling == null ? null : (int) Math.max(0, ceiling - importedToday);
        return new NewsImportSettingsDto(
                settings.isSteamEnabled(),
                settings.isRssEnabled(),
                ceiling,
                settings.getSteamNewsPerGame(),
                importedToday,
                remaining,
                settings.getUpdatedAt(),
                settings.getUpdatedBy()
        );
    }
}
