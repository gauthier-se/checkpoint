package com.seyzeriat.desktop.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO mirroring the API's ImportJobStatusDto for asynchronous bulk imports.
 * Polled by the desktop until the job reaches a terminal state.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportJobStatus {
    private String jobId;
    private String type;
    private String state;
    private int requestedLimit;
    private int minRatingCount;
    private int totalFetched;
    private int processed;
    private int imported;
    private int skipped;
    private int failed;
    private List<String> errors;
    private String errorMessage;

    public ImportJobStatus() {}

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public int getRequestedLimit() { return requestedLimit; }
    public void setRequestedLimit(int requestedLimit) { this.requestedLimit = requestedLimit; }

    public int getMinRatingCount() { return minRatingCount; }
    public void setMinRatingCount(int minRatingCount) { this.minRatingCount = minRatingCount; }

    public int getTotalFetched() { return totalFetched; }
    public void setTotalFetched(int totalFetched) { this.totalFetched = totalFetched; }

    public int getProcessed() { return processed; }
    public void setProcessed(int processed) { this.processed = processed; }

    public int getImported() { return imported; }
    public void setImported(int imported) { this.imported = imported; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /** Whether the job has reached a terminal state. */
    public boolean isTerminal() {
        return "COMPLETED".equals(state) || "FAILED".equals(state);
    }

    /** Whether the job finished successfully. */
    public boolean isFailed() {
        return "FAILED".equals(state);
    }
}
