package com.checkpoint.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.entities.News;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.repositories.NewsRepository;

/**
 * Integration tests covering which news articles the public endpoints expose.
 * Uses H2 plus a real in-heap Lucene index so the search predicates are exercised for real.
 *
 * <p>Regression coverage for #553: an article with no {@code publishedAt} — a draft that was
 * never published, or one that was unpublished again — used to be listed publicly while the
 * single-article endpoint 404'd on it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:newsvisibilitytest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.search.backend.type=lucene",
        "spring.jpa.properties.hibernate.search.backend.directory.type=local-heap"
})
class NewsVisibilityIntegrationTest {

    private static final String PUBLISHED_TITLE = "Elden Ring expansion dated";
    private static final String DRAFT_TITLE = "Never published draft";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsRepository newsRepository;

    private News published;
    private News draft;

    @BeforeEach
    void setUp() {
        newsRepository.deleteAll();

        published = new News();
        published.setTitle(PUBLISHED_TITLE);
        published.setDescription("The expansion lands next spring.");
        published.setSource(NewsSource.MANUAL);
        published.setPublishedAt(LocalDateTime.now().minusDays(1));

        draft = new News();
        draft.setTitle(DRAFT_TITLE);
        draft.setDescription("An expansion draft nobody should see yet.");
        draft.setSource(NewsSource.MANUAL);
        draft.setPublishedAt(null);

        published = newsRepository.save(published);
        draft = newsRepository.save(draft);
    }

    @AfterEach
    void tearDown() {
        newsRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/v1/news")
    class PublicListing {

        @Test
        @DisplayName("should list published articles only")
        void shouldHidePublishedAtNullArticles() throws Exception {
            mockMvc.perform(get("/api/v1/news").param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].title").value(PUBLISHED_TITLE));
        }

        @Test
        @DisplayName("should hide a draft matching the text query")
        void shouldHideDraftFromTextSearch() throws Exception {
            mockMvc.perform(get("/api/v1/news").param("q", "expansion").param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].title").value(PUBLISHED_TITLE));
        }

        @Test
        @DisplayName("should drop an article once it is unpublished")
        void shouldDropUnpublishedArticle() throws Exception {
            published.setPublishedAt(null);
            newsRepository.save(published);

            mockMvc.perform(get("/api/v1/news").param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/news/search")
    class QuickSearch {

        @Test
        @DisplayName("should hide drafts from the quick-search palette")
        void shouldHideDrafts() throws Exception {
            mockMvc.perform(get("/api/v1/news/search").param("q", "expansion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value(PUBLISHED_TITLE));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/news/{id}")
    class SingleArticle {

        @Test
        @DisplayName("should return the published article")
        void shouldReturnPublished() throws Exception {
            mockMvc.perform(get("/api/v1/news/" + published.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value(PUBLISHED_TITLE));
        }

        @Test
        @DisplayName("should 404 on a draft")
        void shouldNotFoundDraft() throws Exception {
            mockMvc.perform(get("/api/v1/news/" + draft.getId()))
                    .andExpect(status().isNotFound());
        }
    }
}
