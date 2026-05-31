package com.seyzeriat.desktop.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a paginated response from the Checkpoint backend.
 *
 * @param <T> the type of content in the page
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedResponse<T> {
    private List<T> content;
    private PageMetadata metadata;

    /**
     * Default constructor for PagedResponse.
     */
    public PagedResponse() {}

    /**
     * Gets the content of the page.
     *
     * @return the list of content items
     */
    public List<T> getContent() { return content; }

    /**
     * Sets the content of the page.
     *
     * @param content the list of content items to set
     */
    public void setContent(List<T> content) { this.content = content; }

    /**
     * Gets the metadata of the page.
     *
     * @return the page metadata
     */
    public PageMetadata getMetadata() { return metadata; }

    /**
     * Sets the metadata of the page.
     *
     * @param metadata the page metadata to set
     */
    public void setMetadata(PageMetadata metadata) { this.metadata = metadata; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageMetadata {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        /**
         * Default constructor for PageMetadata.
         */
        public PageMetadata() {}

        /**
         * Gets the current page number (0-indexed).
         *
         * @return the page number
         */
        public int getPage() { return page; }

        /**
         * Sets the current page number (0-indexed).
         *
         * @param page the page number to set
         */
        public void setPage(int page) { this.page = page; }

        /**
         * Gets the size of the page (number of elements per page).
         *
         * @return the page size
         */
        public int getSize() { return size; }

        /**
         * Sets the size of the page.
         *
         * @param size the page size to set
         */
        public void setSize(int size) { this.size = size; }

        /**
         * Gets the total number of elements across all pages.
         *
         * @return the total elements
         */
        public long getTotalElements() { return totalElements; }

        /**
         * Sets the total number of elements across all pages.
         *
         * @param totalElements the total elements to set
         */
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

        /**
         * Gets the total number of pages.
         *
         * @return the total pages
         */
        public int getTotalPages() { return totalPages; }

        /**
         * Sets the total number of pages.
         *
         * @param totalPages the total pages to set
         */
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
}
