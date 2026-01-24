package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for IGDB Cover response.
 * Contains image information for game covers.
 *
 * @see <a href="https://api-docs.igdb.com/#cover">IGDB Cover Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbCoverDto(
        Long id,

        @JsonProperty("image_id")
        String imageId,

        String url,

        Integer width,

        Integer height,

        @JsonProperty("alpha_channel")
        Boolean alphaChannel,

        Boolean animated
) {
    /**
     * Generates the full URL for the cover image with the specified size.
     * IGDB image URL pattern: https://images.igdb.com/igdb/image/upload/t_{size}/{image_id}.jpg
     *
     * @param size the image size (e.g., "cover_small", "cover_big", "720p", "1080p")
     * @return the full image URL
     */
    public String getImageUrl(String size) {
        if (imageId == null) {
            return null;
        }
        return String.format("https://images.igdb.com/igdb/image/upload/t_%s/%s.jpg", size, imageId);
    }

    /**
     * Gets the cover image URL in "cover_big" size (264x374).
     *
     * @return the cover big URL
     */
    public String getCoverBigUrl() {
        return getImageUrl("cover_big");
    }

    /**
     * Gets the cover image URL in "720p" size (1280x720).
     *
     * @return the 720p URL
     */
    public String get720pUrl() {
        return getImageUrl("720p");
    }
}
