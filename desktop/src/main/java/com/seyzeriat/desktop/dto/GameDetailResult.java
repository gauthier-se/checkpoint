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

    /**
     * Default constructor for Jackson.
     */
    public GameDetailResult() {}

    /**
     * Gets the game ID.
     * @return game ID
     */
    public String getId() { return id; }
    
    /**
     * Sets the game ID.
     * @param id game ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the game title.
     * @return game title
     */
    public String getTitle() { return title; }
    
    /**
     * Sets the game title.
     * @param title game title
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Gets the game description.
     * @return game description
     */
    public String getDescription() { return description; }
    
    /**
     * Sets the game description.
     * @param description game description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Gets the game cover URL.
     * @return cover URL
     */
    public String getCoverUrl() { return coverUrl; }
    
    /**
     * Sets the game cover URL.
     * @param coverUrl cover URL
     */
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    /**
     * Gets the game artwork URL.
     * @return artwork URL
     */
    public String getArtworkUrl() { return artworkUrl; }
    
    /**
     * Sets the game artwork URL.
     * @param artworkUrl artwork URL
     */
    public void setArtworkUrl(String artworkUrl) { this.artworkUrl = artworkUrl; }

    /**
     * Gets the game's YouTube trailer ID.
     * @return trailer YouTube ID
     */
    public String getTrailerYoutubeId() { return trailerYoutubeId; }
    
    /**
     * Sets the game's YouTube trailer ID.
     * @param trailerYoutubeId trailer YouTube ID
     */
    public void setTrailerYoutubeId(String trailerYoutubeId) { this.trailerYoutubeId = trailerYoutubeId; }

    /**
     * Gets the normal time to beat in seconds.
     * @return normal time to beat
     */
    public Long getTimeToBeatNormally() { return timeToBeatNormally; }
    
    /**
     * Sets the normal time to beat in seconds.
     * @param timeToBeatNormally normal time to beat
     */
    public void setTimeToBeatNormally(Long timeToBeatNormally) { this.timeToBeatNormally = timeToBeatNormally; }

    /**
     * Gets the hasty time to beat in seconds.
     * @return hasty time to beat
     */
    public Long getTimeToBeatHastily() { return timeToBeatHastily; }
    
    /**
     * Sets the hasty time to beat in seconds.
     * @param timeToBeatHastily hasty time to beat
     */
    public void setTimeToBeatHastily(Long timeToBeatHastily) { this.timeToBeatHastily = timeToBeatHastily; }

    /**
     * Gets the complete time to beat in seconds.
     * @return complete time to beat
     */
    public Long getTimeToBeatCompletely() { return timeToBeatCompletely; }
    
    /**
     * Sets the complete time to beat in seconds.
     * @param timeToBeatCompletely complete time to beat
     */
    public void setTimeToBeatCompletely(Long timeToBeatCompletely) { this.timeToBeatCompletely = timeToBeatCompletely; }

    /**
     * Gets the game's release date.
     * @return release date
     */
    public LocalDate getReleaseDate() { return releaseDate; }
    
    /**
     * Sets the game's release date.
     * @param releaseDate release date
     */
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    /**
     * Gets the list of genres.
     * @return genres list
     */
    public List<NamedRef> getGenres() { return genres; }
    
    /**
     * Sets the list of genres.
     * @param genres genres list
     */
    public void setGenres(List<NamedRef> genres) { this.genres = genres; }

    /**
     * Gets the list of platforms.
     * @return platforms list
     */
    public List<NamedRef> getPlatforms() { return platforms; }
    
    /**
     * Sets the list of platforms.
     * @param platforms platforms list
     */
    public void setPlatforms(List<NamedRef> platforms) { this.platforms = platforms; }

    /**
     * Gets the list of companies.
     * @return companies list
     */
    public List<NamedRef> getCompanies() { return companies; }
    
    /**
     * Sets the list of companies.
     * @param companies companies list
     */
    public void setCompanies(List<NamedRef> companies) { this.companies = companies; }

    /**
     * Lightweight reference to a related entity by UUID + display name.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NamedRef {
        private String id;
        private String name;

        /**
         * Default constructor for Jackson.
         */
        public NamedRef() {}

        /**
         * Gets the reference ID.
         * @return reference ID
         */
        public String getId() { return id; }
        
        /**
         * Sets the reference ID.
         * @param id reference ID
         */
        public void setId(String id) { this.id = id; }

        /**
         * Gets the reference name.
         * @return reference name
         */
        public String getName() { return name; }
        
        /**
         * Sets the reference name.
         * @param name reference name
         */
        public void setName(String name) { this.name = name; }
    }
}
