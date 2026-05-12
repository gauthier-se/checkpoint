package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for IGDB Game Video response.
 *
 * @see <a href="https://api-docs.igdb.com/#game-video">IGDB Game Video Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbVideoDto(
        Long id,
        String name,

        @JsonProperty("video_id")
        String videoId
) {}
