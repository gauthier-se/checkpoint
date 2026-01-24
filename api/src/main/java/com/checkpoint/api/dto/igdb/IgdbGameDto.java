package com.checkpoint.api.dto.igdb;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for IGDB Game response.
 * Maps the main game structure from IGDB API.
 *
 * @see <a href="https://api-docs.igdb.com/#game">IGDB Game Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbGameDto(
        Long id,
        String name,
        String slug,
        String summary,
        String storyline,

        @JsonProperty("first_release_date")
        Long firstReleaseDate,

        Double rating,

        @JsonProperty("rating_count")
        Integer ratingCount,

        @JsonProperty("aggregated_rating")
        Double aggregatedRating,

        @JsonProperty("aggregated_rating_count")
        Integer aggregatedRatingCount,

        @JsonProperty("total_rating")
        Double totalRating,

        @JsonProperty("total_rating_count")
        Integer totalRatingCount,

        IgdbCoverDto cover,

        List<IgdbGenreDto> genres,

        List<IgdbPlatformDto> platforms,

        @JsonProperty("involved_companies")
        List<IgdbInvolvedCompanyDto> involvedCompanies,

        List<IgdbScreenshotDto> screenshots,

        @JsonProperty("similar_games")
        List<Long> similarGames,

        @JsonProperty("game_modes")
        List<IgdbGameModeDto> gameModes,

        List<IgdbThemeDto> themes,

        @JsonProperty("player_perspectives")
        List<IgdbPlayerPerspectiveDto> playerPerspectives,

        @JsonProperty("parent_game")
        Long parentGame,

        List<Long> dlcs,

        List<Long> expansions,

        @JsonProperty("version_title")
        String versionTitle,

        String url
) {}
