package com.checkpoint.api.jobs;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.services.GameImportService;

/**
 * Executes a bulk-import job on a background thread (the dedicated single-thread
 * {@code importExecutor}). Runs OUTSIDE any transaction — each game is committed
 * independently by {@code GamePersistenceService}. Errors are captured into the
 * job status rather than propagated, since there is no HTTP caller to receive them.
 */
@Service
public class ImportJobRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportJobRunner.class);

    private final IgdbApiClient igdbApiClient;
    private final GameImportService gameImportService;

    public ImportJobRunner(IgdbApiClient igdbApiClient, GameImportService gameImportService) {
        this.igdbApiClient = igdbApiClient;
        this.gameImportService = gameImportService;
    }

    @Async("importExecutor")
    public void run(ImportJobStatus job) {
        log.info("Starting import job {} (type={})", job.getJobId(), job.getType());
        job.setState(JobState.RUNNING);
        try {
            List<IgdbGameDto> games = switch (job.getType()) {
                case TOP_RATED -> igdbApiClient.fetchTopRatedGames(job.getRequestedLimit(), job.getMinRatingCount());
                case RECENT -> igdbApiClient.fetchRecentlyReleasedGames(job.getRequestedLimit());
            };
            job.setTotalFetched(games.size());

            gameImportService.bulkImport(games, job);

            job.setState(JobState.COMPLETED);
            log.info("Import job {} completed", job.getJobId());
        } catch (Exception e) {
            log.error("Import job {} failed: {}", job.getJobId(), e.getMessage(), e);
            job.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            job.setState(JobState.FAILED);
        } finally {
            job.setFinishedAt(Instant.now());
        }
    }
}
