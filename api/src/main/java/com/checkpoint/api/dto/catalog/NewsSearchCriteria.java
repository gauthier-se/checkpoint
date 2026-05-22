package com.checkpoint.api.dto.catalog;

import java.time.LocalDate;
import java.util.UUID;

import com.checkpoint.api.entities.NewsSource;

/**
 * Criteria for searching and filtering news articles.
 * All fields are optional; null values are ignored when building the query.
 *
 * @param q              free-text fuzzy query against title + description
 * @param source         filter by news origin (MANUAL / STEAM / RSS)
 * @param feedName       filter by feed name (exact keyword match, e.g. "IGN")
 * @param videoGameId    filter by linked game
 * @param publishedFrom  inclusive lower bound on publishedAt (start of day)
 * @param publishedTo    inclusive upper bound on publishedAt (end of day)
 * @param sort           "publishedAt,desc" | "publishedAt,asc" | "title,asc" | "title,desc" | "relevance"
 */
public record NewsSearchCriteria(
        String q,
        NewsSource source,
        String feedName,
        UUID videoGameId,
        LocalDate publishedFrom,
        LocalDate publishedTo,
        String sort
) {
    public boolean hasQuery() {
        return q != null && !q.isBlank();
    }
}
