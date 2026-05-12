package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for IGDB Game Time To Beat response.
 * Values are durations expressed in seconds.
 *
 * @see <a href="https://api-docs.igdb.com/#game-time-to-beat">IGDB Game Time To Beat Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbTimeToBeatDto(
        @JsonProperty("game_id")
        Long gameId,

        Long normally,

        Long hastily,

        Long completely
) {}
