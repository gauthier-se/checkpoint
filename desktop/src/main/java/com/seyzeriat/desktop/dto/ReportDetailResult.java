package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing the full details of a report returned by the admin API,
 * including the ID and content of the reported target (review or comment).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportDetailResult {
    private String id;
    private String reporterUsername;
    private String reason;
    private String type;
    private String targetId;
    private String targetAuthorUsername;
    private String targetFullContent;
    private String createdAt;

    /**
     * Default constructor for ReportDetailResult.
     */
    public ReportDetailResult() {}

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
     * Gets the type of the reported target (e.g. review or comment).
     *
     * @return the target type
     */
    public String getType() { return type; }

    /**
     * Sets the type of the reported target.
     *
     * @param type the target type to set
     */
    public void setType(String type) { this.type = type; }

    /**
     * Gets the ID of the reported target.
     *
     * @return the target ID
     */
    public String getTargetId() { return targetId; }

    /**
     * Sets the ID of the reported target.
     *
     * @param targetId the target ID to set
     */
    public void setTargetId(String targetId) { this.targetId = targetId; }

    /**
     * Gets the username of the author of the reported target.
     *
     * @return the target author username
     */
    public String getTargetAuthorUsername() { return targetAuthorUsername; }

    /**
     * Sets the username of the author of the reported target.
     *
     * @param targetAuthorUsername the target author username to set
     */
    public void setTargetAuthorUsername(String targetAuthorUsername) { this.targetAuthorUsername = targetAuthorUsername; }

    /**
     * Gets the full content of the reported target.
     *
     * @return the target full content
     */
    public String getTargetFullContent() { return targetFullContent; }

    /**
     * Sets the full content of the reported target.
     *
     * @param targetFullContent the target full content to set
     */
    public void setTargetFullContent(String targetFullContent) { this.targetFullContent = targetFullContent; }

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
