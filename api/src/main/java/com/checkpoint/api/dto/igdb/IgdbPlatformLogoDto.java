package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for IGDB Platform Logo response.
 *
 * @see <a href="https://api-docs.igdb.com/#platform-logo">IGDB Platform Logo Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbPlatformLogoDto(
        Long id,

        @JsonProperty("image_id")
        String imageId,

        String url,

        Integer width,

        Integer height
) {
    /**
     * Generates the full URL for the platform logo with the specified size.
     *
     * @param size the image size (e.g., "logo_med", "thumb")
     * @return the full image URL
     */
    public String getImageUrl(String size) {
        if (imageId == null) {
            return null;
        }
        return String.format("https://images.igdb.com/igdb/image/upload/t_%s/%s.png", size, imageId);
    }
}
