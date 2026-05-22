package com.seyzeriat.desktop.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO mirroring the API's {@code GameDetailDto}. Used to pre-fill the admin
 * game form in edit mode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDetailResult {
    private String id;
    private String title;
    private String description;
    private String coverUrl;
    private String artworkUrl;
    private String trailerYoutubeId;
    private Long timeToBeatNormally;
    private Long timeToBeatHastily;
    private Long timeToBeatCompletely;
    private LocalDate releaseDate;
    private List<NamedRef> genres;
    private List<NamedRef> platforms;
    private List<NamedRef> companies;

    public GameDetailResult() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public List<NamedRef> getGenres() { return genres; }
    public void setGenres(List<NamedRef> genres) { this.genres = genres; }

    public List<NamedRef> getPlatforms() { return platforms; }
    public void setPlatforms(List<NamedRef> platforms) { this.platforms = platforms; }

    public List<NamedRef> getCompanies() { return companies; }
    public void setCompanies(List<NamedRef> companies) { this.companies = companies; }

    /**
     * Lightweight reference to a related entity by UUID + display name.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NamedRef {
        private String id;
        private String name;

        public NamedRef() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
