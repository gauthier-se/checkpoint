package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing the author of a news article returned by the admin API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsAuthorResult {
    private String id;
    private String pseudo;
    private String picture;

    /**
     * Default constructor for NewsAuthorResult.
     */
    public NewsAuthorResult() {}

    /**
     * Gets the ID of the news author.
     *
     * @return the author ID
     */
    public String getId() { return id; }

    /**
     * Sets the ID of the news author.
     *
     * @param id the author ID to set
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the pseudo or username of the news author.
     *
     * @return the pseudo
     */
    public String getPseudo() { return pseudo; }

    /**
     * Sets the pseudo or username of the news author.
     *
     * @param pseudo the pseudo to set
     */
    public void setPseudo(String pseudo) { this.pseudo = pseudo; }

    /**
     * Gets the profile picture URL of the news author.
     *
     * @return the picture URL
     */
    public String getPicture() { return picture; }

    /**
     * Sets the profile picture URL of the news author.
     *
     * @param picture the picture URL to set
     */
    public void setPicture(String picture) { this.picture = picture; }
}
