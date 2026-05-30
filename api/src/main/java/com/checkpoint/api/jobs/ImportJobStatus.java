package com.checkpoint.api.jobs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.checkpoint.api.dto.admin.ImportJobStatusDto;
import com.checkpoint.api.services.ImportProgressListener;

/**
 * Mutable, thread-safe status of a single asynchronous bulk-import job.
 *
 * <p>The worker thread mutates the counters via the {@link ImportProgressListener}
 * callbacks while HTTP request threads read a {@link #toDto() snapshot}; counters
 * are therefore {@link AtomicInteger}s and lifecycle fields are {@code volatile}.</p>
 */
public class ImportJobStatus implements ImportProgressListener {

    /** Cap on the number of error labels retained, to bound memory. */
    private static final int MAX_ERRORS = 50;

    private final UUID jobId;
    private final ImportType type;
    private final int requestedLimit;
    private final int minRatingCount;

    private volatile JobState state = JobState.PENDING;
    private final AtomicInteger totalFetched = new AtomicInteger();
    private final AtomicInteger processed = new AtomicInteger();
    private final AtomicInteger imported = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private volatile String errorMessage;

    private final Instant startedAt = Instant.now();
    private volatile Instant finishedAt;

    public ImportJobStatus(UUID jobId, ImportType type, int requestedLimit, int minRatingCount) {
        this.jobId = jobId;
        this.type = type;
        this.requestedLimit = requestedLimit;
        this.minRatingCount = minRatingCount;
    }

    public UUID getJobId() {
        return jobId;
    }

    public ImportType getType() {
        return type;
    }

    public int getRequestedLimit() {
        return requestedLimit;
    }

    public int getMinRatingCount() {
        return minRatingCount;
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    public void setTotalFetched(int value) {
        totalFetched.set(value);
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    // ── ImportProgressListener ──────────────────────────────────────────

    @Override
    public void processed() {
        processed.incrementAndGet();
    }

    @Override
    public void imported() {
        imported.incrementAndGet();
    }

    @Override
    public void skipped() {
        skipped.incrementAndGet();
    }

    @Override
    public void failed(String label) {
        failed.incrementAndGet();
        if (label != null && errors.size() < MAX_ERRORS) {
            errors.add(label);
        }
    }

    /**
     * Takes a consistent-enough snapshot of the current counters for serialization.
     */
    public ImportJobStatusDto toDto() {
        return new ImportJobStatusDto(
                jobId.toString(),
                type.name(),
                state.name(),
                requestedLimit,
                minRatingCount,
                totalFetched.get(),
                processed.get(),
                imported.get(),
                skipped.get(),
                failed.get(),
                List.copyOf(errors),
                errorMessage,
                startedAt,
                finishedAt
        );
    }
}
