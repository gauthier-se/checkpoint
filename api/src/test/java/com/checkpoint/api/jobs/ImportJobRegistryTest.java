package com.checkpoint.api.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.checkpoint.api.exceptions.ImportAlreadyRunningException;

/**
 * Unit tests for {@link ImportJobRegistry}: single-import enforcement and lookup.
 */
class ImportJobRegistryTest {

    private final ImportJobRegistry registry = new ImportJobRegistry();

    @Test
    void startJob_thenFind_returnsSameJob() {
        ImportJobStatus job = registry.startJob(ImportType.TOP_RATED, 1000, 50);

        assertThat(registry.find(job.getJobId())).containsSame(job);
    }

    @Test
    void startJob_rejectsSecondWhileFirstActive() {
        registry.startJob(ImportType.TOP_RATED, 1000, 50);

        assertThatThrownBy(() -> registry.startJob(ImportType.RECENT, 100, 0))
                .isInstanceOf(ImportAlreadyRunningException.class);
    }

    @Test
    void startJob_allowsNewJobOncePreviousIsTerminal() {
        ImportJobStatus first = registry.startJob(ImportType.TOP_RATED, 1000, 50);
        first.setState(JobState.COMPLETED);

        ImportJobStatus second = registry.startJob(ImportType.RECENT, 100, 0);
        assertThat(second.getType()).isEqualTo(ImportType.RECENT);
    }

    @Test
    void find_returnsEmptyForUnknownJob() {
        assertThat(registry.find(UUID.randomUUID())).isEmpty();
    }
}
