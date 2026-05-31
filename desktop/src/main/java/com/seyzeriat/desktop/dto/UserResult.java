package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a user returned by the admin API.
 * Maps the JSON response from {@code GET /api/v1/admin/users}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResult {

    private String id;
    private String username;
    private String email;
    private boolean banned;

    /**
     * Default constructor for UserResult.
     */
    public UserResult() {}

    /**
     * Gets the ID of the user.
     *
     * @return the user ID
     */
    public String getId() { return id; }

    /**
     * Sets the ID of the user.
     *
     * @param id the user ID to set
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the username of the user.
     *
     * @return the username
     */
    public String getUsername() { return username; }

    /**
     * Sets the username of the user.
     *
     * @param username the username to set
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Gets the email of the user.
     *
     * @return the email
     */
    public String getEmail() { return email; }

    /**
     * Sets the email of the user.
     *
     * @param email the email to set
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Checks if the user is banned.
     *
     * @return true if the user is banned, false otherwise
     */
    public boolean isBanned() { return banned; }

    /**
     * Sets the banned status of the user.
     *
     * @param banned the banned status to set
     */
    public void setBanned(boolean banned) { this.banned = banned; }
}
