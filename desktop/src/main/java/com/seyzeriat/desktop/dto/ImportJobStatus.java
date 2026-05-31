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

    /**
     * Default constructor for ImportJobStatus.
     */
    public ImportJobStatus() {}

    /**
     * Gets the ID of the import job.
     *
     * @return the job ID
     */
    public String getJobId() { return jobId; }

    /**
     * Sets the ID of the import job.
     *
     * @param jobId the job ID to set
     */
    public void setJobId(String jobId) { this.jobId = jobId; }

    /**
     * Gets the type of the import job.
     *
     * @return the job type
     */
    public String getType() { return type; }

    /**
     * Sets the type of the import job.
     *
     * @param type the job type to set
     */
    public void setType(String type) { this.type = type; }

    /**
     * Gets the state of the import job.
     *
     * @return the job state
     */
    public String getState() { return state; }

    /**
     * Sets the state of the import job.
     *
     * @param state the job state to set
     */
    public void setState(String state) { this.state = state; }

    /**
     * Gets the requested limit of items to import.
     *
     * @return the requested limit
     */
    public int getRequestedLimit() { return requestedLimit; }

    /**
     * Sets the requested limit of items to import.
     *
     * @param requestedLimit the requested limit to set
     */
    public void setRequestedLimit(int requestedLimit) { this.requestedLimit = requestedLimit; }

    /**
     * Gets the minimum rating count required for import.
     *
     * @return the minimum rating count
     */
    public int getMinRatingCount() { return minRatingCount; }

    /**
     * Sets the minimum rating count required for import.
     *
     * @param minRatingCount the minimum rating count to set
     */
    public void setMinRatingCount(int minRatingCount) { this.minRatingCount = minRatingCount; }

    /**
     * Gets the total number of items fetched.
     *
     * @return the total fetched count
     */
    public int getTotalFetched() { return totalFetched; }

    /**
     * Sets the total number of items fetched.
     *
     * @param totalFetched the total fetched count to set
     */
    public void setTotalFetched(int totalFetched) { this.totalFetched = totalFetched; }

    /**
     * Gets the number of items processed so far.
     *
     * @return the processed count
     */
    public int getProcessed() { return processed; }

    /**
     * Sets the number of items processed.
     *
     * @param processed the processed count to set
     */
    public void setProcessed(int processed) { this.processed = processed; }

    /**
     * Gets the number of items successfully imported.
     *
     * @return the imported count
     */
    public int getImported() { return imported; }

    /**
     * Sets the number of items successfully imported.
     *
     * @param imported the imported count to set
     */
    public void setImported(int imported) { this.imported = imported; }

    /**
     * Gets the number of items skipped during import.
     *
     * @return the skipped count
     */
    public int getSkipped() { return skipped; }

    /**
     * Sets the number of items skipped during import.
     *
     * @param skipped the skipped count to set
     */
    public void setSkipped(int skipped) { this.skipped = skipped; }

    /**
     * Gets the number of items that failed to import.
     *
     * @return the failed count
     */
    public int getFailed() { return failed; }

    /**
     * Sets the number of items that failed to import.
     *
     * @param failed the failed count to set
     */
    public void setFailed(int failed) { this.failed = failed; }

    /**
     * Gets the list of error messages encountered during import.
     *
     * @return the list of errors
     */
    public List<String> getErrors() { return errors; }

    /**
     * Sets the list of error messages encountered during import.
     *
     * @param errors the list of errors to set
     */
    public void setErrors(List<String> errors) { this.errors = errors; }

    /**
     * Gets a general error message if the job failed as a whole.
     *
     * @return the general error message
     */
    public String getErrorMessage() { return errorMessage; }

    /**
     * Sets a general error message for the job.
     *
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /**
     * Checks whether the job has reached a terminal state (COMPLETED or FAILED).
     *
     * @return true if terminal, false otherwise
     */
    public boolean isTerminal() {
        return "COMPLETED".equals(state) || "FAILED".equals(state);
    }

    /**
     * Checks whether the job finished with a FAILED state.
     *
     * @return true if failed, false otherwise
     */
    public boolean isFailed() {
        return "FAILED".equals(state);
    }
}
