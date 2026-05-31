package com.seyzeriat.desktop.dto;

import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request payload used for both the admin create and update endpoints.
 * Field names mirror the API DTOs.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class GameFormPayload {
    private String title;
    private String description;
    private String coverUrl;
    private String artworkUrl;
    private String trailerYoutubeId;
    private Long timeToBeatNormally;
    private Long timeToBeatHastily;
    private Long timeToBeatCompletely;
    private LocalDate releaseDate;
    private Set<String> genreIds;
    private Set<String> platformIds;
    private Set<String> companyIds;

    /**
     * Default constructor for GameFormPayload.
     */
    public GameFormPayload() {}

    /**
     * Gets the title of the game.
     *
     * @return the title
     */
    public String getTitle() { return title; }

    /**
     * Sets the title of the game.
     *
     * @param title the title to set
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Gets the description of the game.
     *
     * @return the description
     */
    public String getDescription() { return description; }

    /**
     * Sets the description of the game.
     *
     * @param description the description to set
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Gets the cover URL of the game.
     *
     * @return the cover URL
     */
    public String getCoverUrl() { return coverUrl; }

    /**
     * Sets the cover URL of the game.
     *
     * @param coverUrl the cover URL to set
     */
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    /**
     * Gets the artwork URL of the game.
     *
     * @return the artwork URL
     */
    public String getArtworkUrl() { return artworkUrl; }

    /**
     * Sets the artwork URL of the game.
     *
     * @param artworkUrl the artwork URL to set
     */
    public void setArtworkUrl(String artworkUrl) { this.artworkUrl = artworkUrl; }

    /**
     * Gets the YouTube ID of the game trailer.
     *
     * @return the YouTube ID
     */
    public String getTrailerYoutubeId() { return trailerYoutubeId; }

    /**
     * Sets the YouTube ID of the game trailer.
     *
     * @param trailerYoutubeId the YouTube ID to set
     */
    public void setTrailerYoutubeId(String trailerYoutubeId) { this.trailerYoutubeId = trailerYoutubeId; }

    /**
     * Gets the time to beat the game normally.
     *
     * @return the normal time to beat in seconds
     */
    public Long getTimeToBeatNormally() { return timeToBeatNormally; }

    /**
     * Sets the time to beat the game normally.
     *
     * @param timeToBeatNormally the normal time to beat to set
     */
    public void setTimeToBeatNormally(Long timeToBeatNormally) { this.timeToBeatNormally = timeToBeatNormally; }

    /**
     * Gets the time to beat the game hastily.
     *
     * @return the hasty time to beat in seconds
     */
    public Long getTimeToBeatHastily() { return timeToBeatHastily; }

    /**
     * Sets the time to beat the game hastily.
     *
     * @param timeToBeatHastily the hasty time to beat to set
     */
    public void setTimeToBeatHastily(Long timeToBeatHastily) { this.timeToBeatHastily = timeToBeatHastily; }

    /**
     * Gets the time to beat the game completely.
     *
     * @return the complete time to beat in seconds
     */
    public Long getTimeToBeatCompletely() { return timeToBeatCompletely; }

    /**
     * Sets the time to beat the game completely.
     *
     * @param timeToBeatCompletely the complete time to beat to set
     */
    public void setTimeToBeatCompletely(Long timeToBeatCompletely) { this.timeToBeatCompletely = timeToBeatCompletely; }

    /**
     * Gets the release date of the game.
     *
     * @return the release date
     */
    public LocalDate getReleaseDate() { return releaseDate; }

    /**
     * Sets the release date of the game.
     *
     * @param releaseDate the release date to set
     */
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    /**
     * Gets the set of genre IDs associated with the game.
     *
     * @return the set of genre IDs
     */
    public Set<String> getGenreIds() { return genreIds; }

    /**
     * Sets the set of genre IDs associated with the game.
     *
     * @param genreIds the set of genre IDs to set
     */
    public void setGenreIds(Set<String> genreIds) { this.genreIds = genreIds; }

    /**
     * Gets the set of platform IDs associated with the game.
     *
     * @return the set of platform IDs
     */
    public Set<String> getPlatformIds() { return platformIds; }

    /**
     * Sets the set of platform IDs associated with the game.
     *
     * @param platformIds the set of platform IDs to set
     */
    public void setPlatformIds(Set<String> platformIds) { this.platformIds = platformIds; }

    /**
     * Gets the set of company IDs associated with the game.
     *
     * @return the set of company IDs
     */
    public Set<String> getCompanyIds() { return companyIds; }

    /**
     * Sets the set of company IDs associated with the game.
     *
     * @param companyIds the set of company IDs to set
     */
    public void setCompanyIds(Set<String> companyIds) { this.companyIds = companyIds; }
}
