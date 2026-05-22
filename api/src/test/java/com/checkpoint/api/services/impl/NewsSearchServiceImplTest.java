package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.NewsAuthorDto;
import com.checkpoint.api.dto.catalog.NewsResponseDto;
import com.checkpoint.api.dto.catalog.NewsSearchCriteria;
import com.checkpoint.api.entities.News;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.mapper.NewsMapper;

import jakarta.persistence.EntityManager;

/**
 * Unit tests for {@link NewsSearchServiceImpl}. Mirrors the mocking pattern used in
 * {@code GameSearchServiceImplTest} — the full Hibernate Search DSL is mocked end-to-end.
 */
@ExtendWith(MockitoExtension.class)
class NewsSearchServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private NewsMapper newsMapper;

    private NewsSearchServiceImpl newsSearchService;

    @BeforeEach
    void setUp() {
        newsSearchService = new NewsSearchServiceImpl(entityManager, newsMapper);
    }

    private NewsResponseDto buildDto(UUID id, String title) {
        return new NewsResponseDto(
                id, title, "desc", null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
                new NewsAuthorDto(UUID.randomUUID(), "admin", null),
                NewsSource.MANUAL, null, null, null
        );
    }

    /**
     * Builds a mocked SearchSession whose paginated fetch returns the supplied hits + total.
     * Uses raw types and {@code doReturn} to bypass Hibernate Search's generic fluent API.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private SearchSession mockPaginatedSession(List<News> hits, long total) {
        SearchSession session = mock(SearchSession.class);
        SearchQuerySelectStep selectStep = mock(SearchQuerySelectStep.class);
        SearchQueryOptionsStep optionsStep = mock(SearchQueryOptionsStep.class);
        SearchResult<News> result = mock(SearchResult.class);
        SearchResultTotal resultTotal = mock(SearchResultTotal.class);

        Mockito.doReturn(selectStep).when(session).search(any(Class.class));
        Mockito.doReturn(optionsStep).when(selectStep).where(any(java.util.function.Function.class));
        Mockito.doReturn(optionsStep).when(optionsStep).sort(any(java.util.function.Function.class));
        Mockito.doReturn(result).when(optionsStep).fetch(anyInt(), anyInt());
        when(result.hits()).thenReturn(hits);
        when(result.total()).thenReturn(resultTotal);
        when(resultTotal.hitCount()).thenReturn(total);

        return session;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SearchSession mockQuickSearchSession(List<News> hits) {
        SearchSession session = mock(SearchSession.class);
        SearchQuerySelectStep selectStep = mock(SearchQuerySelectStep.class);
        SearchQueryOptionsStep optionsStep = mock(SearchQueryOptionsStep.class);

        Mockito.doReturn(selectStep).when(session).search(any(Class.class));
        Mockito.doReturn(optionsStep).when(selectStep).where(any(java.util.function.Function.class));
        Mockito.doReturn(hits).when(optionsStep).fetchHits(anyInt());

        return session;
    }

    @Test
    @DisplayName("search should map hits to DTOs and preserve total count")
    void search_shouldReturnMappedPage() {
        UUID id = UUID.randomUUID();
        News news = new News();
        news.setId(id);
        news.setTitle("Elden Ring DLC announced");

        NewsResponseDto dto = buildDto(id, "Elden Ring DLC announced");
        when(newsMapper.toDto(news)).thenReturn(dto);

        SearchSession session = mockPaginatedSession(List.of(news), 1L);

        try (MockedStatic<org.hibernate.search.mapper.orm.Search> searchStatic =
                     Mockito.mockStatic(org.hibernate.search.mapper.orm.Search.class)) {
            searchStatic.when(() -> org.hibernate.search.mapper.orm.Search.session(entityManager))
                    .thenReturn(session);

            NewsSearchCriteria criteria = new NewsSearchCriteria(
                    "elden", null, null, null, null, null, null
            );
            Pageable pageable = PageRequest.of(0, 12);
            Page<NewsResponseDto> page = newsSearchService.search(criteria, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1L);
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).id()).isEqualTo(id);
            assertThat(page.getContent().get(0).title()).isEqualTo("Elden Ring DLC announced");
        }
    }

    @Test
    @DisplayName("search should return empty page when no matches")
    void search_shouldReturnEmptyPage() {
        SearchSession session = mockPaginatedSession(List.of(), 0L);

        try (MockedStatic<org.hibernate.search.mapper.orm.Search> searchStatic =
                     Mockito.mockStatic(org.hibernate.search.mapper.orm.Search.class)) {
            searchStatic.when(() -> org.hibernate.search.mapper.orm.Search.session(entityManager))
                    .thenReturn(session);

            NewsSearchCriteria criteria = new NewsSearchCriteria(
                    "nothing matches", null, null, null, null, null, "publishedAt,desc"
            );
            Page<NewsResponseDto> page = newsSearchService.search(criteria, PageRequest.of(0, 20));

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }
    }

    @Test
    @DisplayName("search should accept full criteria with filters and date range")
    void search_shouldAcceptFullCriteria() {
        SearchSession session = mockPaginatedSession(List.of(), 0L);

        try (MockedStatic<org.hibernate.search.mapper.orm.Search> searchStatic =
                     Mockito.mockStatic(org.hibernate.search.mapper.orm.Search.class)) {
            searchStatic.when(() -> org.hibernate.search.mapper.orm.Search.session(entityManager))
                    .thenReturn(session);

            NewsSearchCriteria criteria = new NewsSearchCriteria(
                    null, NewsSource.RSS, "IGN", UUID.randomUUID(),
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1),
                    "title,asc"
            );
            Page<NewsResponseDto> page = newsSearchService.search(criteria, PageRequest.of(0, 5));

            assertThat(page).isNotNull();
            assertThat(page.getContent()).isEmpty();
        }
    }

    @Test
    @DisplayName("quickSearch should return empty list for blank query")
    void quickSearch_shouldReturnEmptyForBlank() {
        assertThat(newsSearchService.quickSearch(null, 5)).isEmpty();
        assertThat(newsSearchService.quickSearch("", 5)).isEmpty();
        assertThat(newsSearchService.quickSearch("   ", 5)).isEmpty();
    }

    @Test
    @DisplayName("quickSearch should map hits to DTOs")
    void quickSearch_shouldReturnMappedHits() {
        UUID id = UUID.randomUUID();
        News news = new News();
        news.setId(id);
        NewsResponseDto dto = buildDto(id, "Quick result");
        when(newsMapper.toDto(news)).thenReturn(dto);

        SearchSession session = mockQuickSearchSession(List.of(news));

        try (MockedStatic<org.hibernate.search.mapper.orm.Search> searchStatic =
                     Mockito.mockStatic(org.hibernate.search.mapper.orm.Search.class)) {
            searchStatic.when(() -> org.hibernate.search.mapper.orm.Search.session(entityManager))
                    .thenReturn(session);

            List<NewsResponseDto> results = newsSearchService.quickSearch("elden", 3);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo(id);
        }
    }
}
