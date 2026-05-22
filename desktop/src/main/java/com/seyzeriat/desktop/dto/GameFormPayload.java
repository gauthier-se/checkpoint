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

    public GameFormPayload() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getArtworkUrl() { return artworkUrl; }
    public void setArtworkUrl(String artworkUrl) { this.artworkUrl = artworkUrl; }

    public String getTrailerYoutubeId() { return trailerYoutubeId; }
    public void setTrailerYoutubeId(String trailerYoutubeId) { this.trailerYoutubeId = trailerYoutubeId; }

    public Long getTimeToBeatNormally() { return timeToBeatNormally; }
    public void setTimeToBeatNormally(Long timeToBeatNormally) { this.timeToBeatNormally = timeToBeatNormally; }

    public Long getTimeToBeatHastily() { return timeToBeatHastily; }
    public void setTimeToBeatHastily(Long timeToBeatHastily) { this.timeToBeatHastily = timeToBeatHastily; }

    public Long getTimeToBeatCompletely() { return timeToBeatCompletely; }
    public void setTimeToBeatCompletely(Long timeToBeatCompletely) { this.timeToBeatCompletely = timeToBeatCompletely; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public Set<String> getGenreIds() { return genreIds; }
    public void setGenreIds(Set<String> genreIds) { this.genreIds = genreIds; }

    public Set<String> getPlatformIds() { return platformIds; }
    public void setPlatformIds(Set<String> platformIds) { this.platformIds = platformIds; }

    public Set<String> getCompanyIds() { return companyIds; }
    public void setCompanyIds(Set<String> companyIds) { this.companyIds = companyIds; }
}
