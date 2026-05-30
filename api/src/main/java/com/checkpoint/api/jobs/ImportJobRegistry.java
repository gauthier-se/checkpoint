package com.checkpoint.api.jobs;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.checkpoint.api.exceptions.ImportAlreadyRunningException;

/**
 * In-memory registry of asynchronous bulk-import jobs.
 *
 * <p>A single admin instance is expected, so an in-memory map is sufficient;
 * jobs are lost on restart (the desktop treats a missing job as terminal).
 * Only one import may run at a time — {@link #startJob} atomically refuses to
 * create a second job while one is pending or running.</p>
 */
@Component
public class ImportJobRegistry {

    private static final Logger log = LoggerFactory.getLogger(ImportJobRegistry.class);

    /** Terminal jobs older than this are evicted by the cleanup sweep. */
    private static final Duration RETENTION = Duration.ofHours(1);

    private final ConcurrentHashMap<UUID, ImportJobStatus> jobs = new ConcurrentHashMap<>();

    /**
     * Atomically creates a new job, rejecting the request if one is already
     * pending or running.
     *
     * @throws ImportAlreadyRunningException if another import is in progress
     */
    public synchronized ImportJobStatus startJob(ImportType type, int requestedLimit, int minRatingCount) {
        if (hasActiveJob()) {
            throw new ImportAlreadyRunningException();
        }
        ImportJobStatus job = new ImportJobStatus(UUID.randomUUID(), type, requestedLimit, minRatingCount);
        jobs.put(job.getJobId(), job);
        log.info("Created import job {} (type={}, limit={})", job.getJobId(), type, requestedLimit);
        return job;
    }

    public Optional<ImportJobStatus> find(UUID jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    private boolean hasActiveJob() {
        return jobs.values().stream()
                .anyMatch(j -> j.getState() == JobState.PENDING || j.getState() == JobState.RUNNING);
    }

    /**
     * Periodically evicts terminal jobs that finished more than {@link #RETENTION} ago,
     * so the map does not grow unbounded.
     */
    @Scheduled(fixedDelay = 3_600_000L)
    public void evictOldJobs() {
        Instant cutoff = Instant.now().minus(RETENTION);
        jobs.values().removeIf(j -> j.getFinishedAt() != null && j.getFinishedAt().isBefore(cutoff));
    }
}
