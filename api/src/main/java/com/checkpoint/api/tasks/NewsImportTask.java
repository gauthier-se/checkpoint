package com.checkpoint.api.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.checkpoint.api.services.NewsImportService;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Scheduled passes that pull news from Steam (per-game) and from configured RSS feeds
 * into the local {@code news} table. Both passes are dedup-safe (see
 * {@link com.checkpoint.api.repositories.NewsRepository#existsBySourceAndExternalId}).
 */
@Component
public class NewsImportTask {

    private static final Logger log = LoggerFactory.getLogger(NewsImportTask.class);

    private final NewsImportService newsImportService;

    public NewsImportTask(NewsImportService newsImportService) {
        this.newsImportService = newsImportService;
    }

    /**
     * Every 6 hours at minute 0. Steam News rate-limits at 1 req/s in the client,
     * so a library of 200 games takes ~3 minutes — well within the cron window.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @SchedulerLock(name = "newsImportSteam", lockAtLeastFor = "30m", lockAtMostFor = "2h")
    public void runSteamPass() {
        log.info("Scheduled Steam news pass starting");
        try {
            int imported = newsImportService.importSteamNews();
            log.info("Scheduled Steam news pass finished: {} new entries", imported);
        } catch (Exception e) {
            log.error("Scheduled Steam news pass failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Every hour at minute 30. Each feed fetch has a 10s timeout in the client, so a
     * handful of feeds finishes well under a minute even if one is sluggish.
     */
    @Scheduled(cron = "0 30 * * * *")
    @SchedulerLock(name = "newsImportRss", lockAtLeastFor = "5m", lockAtMostFor = "20m")
    public void runRssPass() {
        log.info("Scheduled RSS news pass starting");
        try {
            int imported = newsImportService.importRssFeeds();
            log.info("Scheduled RSS news pass finished: {} new entries", imported);
        } catch (Exception e) {
            log.error("Scheduled RSS news pass failed: {}", e.getMessage(), e);
        }
    }
}
