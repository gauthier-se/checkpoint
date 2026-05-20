package com.checkpoint.api.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.client.RssFeedClient;
import com.checkpoint.api.client.SteamNewsApiClient;
import com.checkpoint.api.config.RssFeedsProperties;
import com.checkpoint.api.dto.steam.SteamNewsResponseDto;
import com.checkpoint.api.entities.News;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.repositories.NewsRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.NewsImportService;

/**
 * Implementation of {@link NewsImportService}.
 *
 * <p>Each save is unwrapped from any service-level transaction so that a single
 * poison item — bad payload, dedup race, etc. — cannot roll back the rest of the
 * batch. Per-item and per-source error isolation are explicit in the code below.</p>
 */
@Service
public class NewsImportServiceImpl implements NewsImportService {

    private static final Logger log = LoggerFactory.getLogger(NewsImportServiceImpl.class);

    private static final int STEAM_NEWS_PER_GAME = 5;
    private static final String STEAM_FEED_NAME = "Steam Community";

    private final SteamNewsApiClient steamNewsApiClient;
    private final RssFeedClient rssFeedClient;
    private final IgdbApiClient igdbApiClient;
    private final VideoGameRepository videoGameRepository;
    private final NewsRepository newsRepository;
    private final RssFeedsProperties rssFeedsProperties;

    public NewsImportServiceImpl(SteamNewsApiClient steamNewsApiClient,
                                 RssFeedClient rssFeedClient,
                                 IgdbApiClient igdbApiClient,
                                 VideoGameRepository videoGameRepository,
                                 NewsRepository newsRepository,
                                 RssFeedsProperties rssFeedsProperties) {
        this.steamNewsApiClient = steamNewsApiClient;
        this.rssFeedClient = rssFeedClient;
        this.igdbApiClient = igdbApiClient;
        this.videoGameRepository = videoGameRepository;
        this.newsRepository = newsRepository;
        this.rssFeedsProperties = rssFeedsProperties;
    }

    @Override
    public int importSteamNews() {
        List<VideoGame> games = videoGameRepository.findGamesWithAtLeastOneUserLink();
        if (games.isEmpty()) {
            log.debug("Steam news import: no games in any user library");
            return 0;
        }

        backfillSteamAppIds(games);

        int imported = 0;
        for (VideoGame game : games) {
            if (game.getSteamAppId() == null) {
                continue;
            }
            try {
                imported += importSteamNewsForGame(game);
            } catch (Exception e) {
                log.warn("Steam news import failed for game '{}' (steamAppId={}): {}",
                        game.getTitle(), game.getSteamAppId(), e.getMessage());
            }
        }
        log.info("Steam news import: {} new entries across {} games", imported, games.size());
        return imported;
    }

    private void backfillSteamAppIds(List<VideoGame> games) {
        List<Long> missingIgdbIds = new ArrayList<>();
        Map<Long, VideoGame> byIgdbId = new java.util.HashMap<>();
        for (VideoGame g : games) {
            if (g.getSteamAppId() == null && g.getIgdbId() != null) {
                missingIgdbIds.add(g.getIgdbId());
                byIgdbId.put(g.getIgdbId(), g);
            }
        }
        if (missingIgdbIds.isEmpty()) {
            return;
        }

        try {
            Map<Long, Long> resolved = igdbApiClient.findSteamAppIdsForIgdbIds(missingIgdbIds);
            for (Map.Entry<Long, Long> entry : resolved.entrySet()) {
                VideoGame game = byIgdbId.get(entry.getKey());
                if (game != null) {
                    game.setSteamAppId(entry.getValue());
                    videoGameRepository.save(game);
                }
            }
            log.info("Steam appId backfill: resolved {}/{} games",
                    resolved.size(), missingIgdbIds.size());
        } catch (Exception e) {
            log.warn("Steam appId backfill failed: {}", e.getMessage());
        }
    }

    private int importSteamNewsForGame(VideoGame game) {
        List<SteamNewsResponseDto.NewsItem> items =
                steamNewsApiClient.fetchNewsForApp(game.getSteamAppId(), STEAM_NEWS_PER_GAME);

        int imported = 0;
        for (SteamNewsResponseDto.NewsItem item : items) {
            try {
                if (item.gid() == null || item.title() == null) {
                    continue;
                }
                if (newsRepository.existsBySourceAndExternalId(NewsSource.STEAM, item.gid())) {
                    continue;
                }
                News news = new News(
                        item.title(),
                        item.contents(),
                        NewsSource.STEAM,
                        item.gid(),
                        item.url(),
                        STEAM_FEED_NAME,
                        game
                );
                news.setPublishedAt(LocalDateTime.now());
                newsRepository.save(news);
                imported++;
            } catch (Exception e) {
                log.warn("Steam news item skipped (gid={}): {}", item.gid(), e.getMessage());
            }
        }
        return imported;
    }

    @Override
    public int importRssFeeds() {
        List<RssFeedsProperties.Feed> feeds = rssFeedsProperties.getFeeds();
        if (feeds == null || feeds.isEmpty()) {
            log.debug("RSS import: no feeds configured");
            return 0;
        }

        int imported = 0;
        for (RssFeedsProperties.Feed feed : feeds) {
            try {
                imported += importOneRssFeed(feed);
            } catch (Exception e) {
                log.warn("RSS feed '{}' failed: {}", feed.getName(), e.getMessage());
            }
        }
        log.info("RSS import: {} new entries across {} feed(s)", imported, feeds.size());
        return imported;
    }

    private int importOneRssFeed(RssFeedsProperties.Feed feed) {
        List<RssFeedClient.RssItem> items = rssFeedClient.fetch(feed.getName(), feed.getUrl());

        int imported = 0;
        for (RssFeedClient.RssItem item : items) {
            try {
                if (item.guid() == null || item.title() == null) {
                    continue;
                }
                if (newsRepository.existsBySourceAndExternalId(NewsSource.RSS, item.guid())) {
                    continue;
                }
                News news = new News(
                        item.title(),
                        item.description(),
                        NewsSource.RSS,
                        item.guid(),
                        item.link(),
                        feed.getName(),
                        null
                );
                news.setPicture(item.imageUrl());
                news.setPublishedAt(LocalDateTime.now());
                newsRepository.save(news);
                imported++;
            } catch (Exception e) {
                log.warn("RSS item skipped (feed='{}', guid={}): {}",
                        feed.getName(), item.guid(), e.getMessage());
            }
        }
        return imported;
    }

    @Override
    public int importFromSource(NewsSource source) {
        return switch (source) {
            case STEAM -> importSteamNews();
            case RSS -> importRssFeeds();
            case MANUAL -> throw new IllegalArgumentException(
                    "Cannot import from MANUAL — manual news is created via the admin panel");
        };
    }
}
