package com.checkpoint.api.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for IGDB Screenshot response.
 *
 * @see <a href="https://api-docs.igdb.com/#screenshot">IGDB Screenshot Endpoint</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbScreenshotDto(
        Long id,

        @JsonProperty("image_id")
        String imageId,

        String url,

        Integer width,

        Integer height
) {
    /**
     * Generates the full URL for the screenshot with the specified size.
     *
     * @param size the image size (e.g., "screenshot_med", "screenshot_big", "720p", "1080p")
     * @return the full image URL
     */
    public String getImageUrl(String size) {
        if (imageId == null) {
            return null;
        }
        return String.format("https://images.igdb.com/igdb/image/upload/t_%s/%s.jpg", size, imageId);
    }

    /**
     * Gets the screenshot URL in "720p" size.
     *
     * @return the 720p URL
     */
    public String get720pUrl() {
        return getImageUrl("720p");
    }
}
