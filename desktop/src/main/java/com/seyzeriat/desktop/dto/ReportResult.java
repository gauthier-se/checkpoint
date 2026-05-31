package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a report returned by the admin API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportResult {
    private String id;
    private String reporterUsername;
    private String reason;
    private String type;
    private String contentPreview;
    private String createdAt;

    /**
     * Default constructor for Jackson.
     */
    public ReportResult() {}

    /**
     * Gets the report ID.
     * @return the report ID
     */
    public String getId() { return id; }
    
    /**
     * Sets the report ID.
     * @param id the report ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the username of the user who made the report.
     * @return the reporter username
     */
    public String getReporterUsername() { return reporterUsername; }
    
    /**
     * Sets the username of the user who made the report.
     * @param reporterUsername the reporter username
     */
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }

    /**
     * Gets the reason for the report.
     * @return the report reason
     */
    public String getReason() { return reason; }
    
    /**
     * Sets the reason for the report.
     * @param reason the report reason
     */
    public void setReason(String reason) { this.reason = reason; }

    /**
     * Gets the type of the report.
     * @return the report type
     */
    public String getType() { return type; }
    
    /**
     * Sets the type of the report.
     * @param type the report type
     */
    public void setType(String type) { this.type = type; }

    /**
     * Gets the preview content of what is reported.
     * @return the content preview
     */
    public String getContentPreview() { return contentPreview; }
    
    /**
     * Sets the preview content of what is reported.
     * @param contentPreview the content preview
     */
    public void setContentPreview(String contentPreview) { this.contentPreview = contentPreview; }

    /**
     * Gets the creation timestamp of the report.
     * @return the creation timestamp
     */
    public String getCreatedAt() { return createdAt; }
    
    /**
     * Sets the creation timestamp of the report.
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
