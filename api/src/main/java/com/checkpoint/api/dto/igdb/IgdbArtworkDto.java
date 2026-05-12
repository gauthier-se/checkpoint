package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for IGDB Artwork response.
 *
 * @see <a href="https://api-docs.igdb.com/#artwork">IGDB Artwork Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbArtworkDto(
        Long id,

        @JsonProperty("image_id")
        String imageId,

        String url,

        Integer width,

        Integer height
) {
    /**
     * Generates the full URL for the artwork with the specified size.
     *
     * @param size the image size (e.g., "screenshot_huge", "1080p", "720p")
     * @return the full image URL
     */
    public String getImageUrl(String size) {
        if (imageId == null) {
            return null;
        }
        return String.format("https://images.igdb.com/igdb/image/upload/t_%s/%s.jpg", size, imageId);
    }

    /**
     * Gets the artwork URL in "1080p" size.
     *
     * @return the 1080p URL
     */
    public String get1080pUrl() {
        return getImageUrl("1080p");
    }
}
