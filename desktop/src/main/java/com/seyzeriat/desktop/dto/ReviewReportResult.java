package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a single report filed against a review,
 * returned by the admin API when inspecting a review's reports.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewReportResult {
    private String id;
    private String reporterUsername;
    private String reason;
    private String createdAt;

    /**
     * Default constructor for ReviewReportResult.
     */
    public ReviewReportResult() {}

    /**
     * Gets the ID of the report.
     *
     * @return the report ID
     */
    public String getId() { return id; }

    /**
     * Sets the ID of the report.
     *
     * @param id the report ID to set
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the username of the reporter.
     *
     * @return the reporter username
     */
    public String getReporterUsername() { return reporterUsername; }

    /**
     * Sets the username of the reporter.
     *
     * @param reporterUsername the reporter username to set
     */
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }

    /**
     * Gets the reason for the report.
     *
     * @return the reason
     */
    public String getReason() { return reason; }

    /**
     * Sets the reason for the report.
     *
     * @param reason the reason to set
     */
    public void setReason(String reason) { this.reason = reason; }

    /**
     * Gets the creation date of the report.
     *
     * @return the created at date
     */
    public String getCreatedAt() { return createdAt; }

    /**
     * Sets the creation date of the report.
     *
     * @param createdAt the created at date to set
     */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
