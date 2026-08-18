package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

import com.checkpoint.api.dto.list.GameListCardDto;
import com.checkpoint.api.dto.list.GameListSearchCriteria;
import com.checkpoint.api.entities.GameList;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.mapper.GameListMapper;
import com.checkpoint.api.repositories.CommentRepository;
import com.checkpoint.api.repositories.GameListEntryRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * Unit tests for {@link ListSearchServiceImpl}. Mirrors the mocking pattern from
 * {@code NewsSearchServiceImplTest}: the full Hibernate Search DSL is mocked end-to-end.
 */
@ExtendWith(MockitoExtension.class)
class ListSearchServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private GameListEntryRepository gameListEntryRepository;

    @Mock
    private GameListMapper gameListMapper;

    private ListSearchServiceImpl listSearchService;

    @BeforeEach
    void setUp() {
        listSearchService = new ListSearchServiceImpl(
                entityManager, userRepository, likeRepository,
                commentRepository, gameListEntryRepository, gameListMapper);
    }

    private GameListCardDto buildDto(UUID id, String title) {
        return new GameListCardDto(
                id, title, "desc", false,
                3, 5L, 1L, "alice", null,
                List.of(), LocalDateTime.now());
    }

    private GameList buildList(UUID id, String pseudo) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPseudo(pseudo);
        GameList list = new GameList();
        list.setId(id);
        list.setUser(user);
        return list;
    }

    /**
     * Builds a mocked SearchSession whose paginated fetch returns the supplied hits + total.
     * Uses raw types and {@code doReturn} to bypass Hibernate Search's generic fluent API.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private SearchSession mockPaginatedSession(List<GameList> hits, long total) {
        SearchSession session = mock(SearchSession.class);
        SearchQuerySelectStep selectStep = mock(SearchQuerySelectStep.class);
        SearchQueryOptionsStep optionsStep = mock(SearchQueryOptionsStep.class);
        SearchResult<GameList> result = mock(SearchResult.class);
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

    @Test
    @DisplayName("search should map hits to card DTOs and preserve total count")
    void search_shouldReturnMappedPage() {
        UUID id = UUID.randomUUID();
        GameList list = buildList(id, "alice");

        GameListCardDto dto = buildDto(id, "Best RPGs");
        when(likeRepository.countByGameListId(id)).thenReturn(5L);
        when(commentRepository.countByGameListId(id)).thenReturn(1L);
        when(gameListEntryRepository.findByGameListIdOrderByPositionAsc(id)).thenReturn(List.of());
        when(gameListMapper.toCardDto(eq(list), eq(5L), eq(1L), any())).thenReturn(dto);

        SearchSession session = mockPaginatedSession(List.of(list), 1L);

        try (MockedStatic<org.hibernate.search.mapper.orm.Search> searchStatic =
                     Mockito.mockStatic(org.hibernate.search.mapper.orm.Search.class)) {
            searchStatic.when(() -> org.hibernate.search.mapper.orm.Search.session(entityManager))
                    .thenReturn(session);

            GameListSearchCriteria criteria = new GameListSearchCriteria(
                    "beste", null, null, null, null);
            Pageable pageable = PageRequest.of(0, 12);
            Page<GameListCardDto> page = listSearchService.search(criteria, pageable, null);

            assertThat(page.getTotalElements()).isEqualTo(1L);
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).id()).isEqualTo(id);
            assertThat(page.getContent().get(0).title()).isEqualTo("Best RPGs");
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

            GameListSearchCriteria criteria = new GameListSearchCriteria(
                    "nothing", "recent", null, null, null);
            Page<GameListCardDto> page = listSearchService.search(
                    criteria, PageRequest.of(0, 20), null);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }
    }

    @Test
    @DisplayName("search should accept full filter set with most-games sort")
    void search_shouldAcceptFullCriteria() {
        SearchSession session = mockPaginatedSession(List.of(), 0L);

        try (MockedStatic<org.hibernate.search.mapper.orm.Search> searchStatic =
                     Mockito.mockStatic(org.hibernate.search.mapper.orm.Search.class)) {
            searchStatic.when(() -> org.hibernate.search.mapper.orm.Search.session(entityManager))
                    .thenReturn(session);

            GameListSearchCriteria criteria = new GameListSearchCriteria(
                    null, "most-games", "public", "alice", 10);
            Page<GameListCardDto> page = listSearchService.search(
                    criteria, PageRequest.of(0, 5), null);

            assertThat(page).isNotNull();
            assertThat(page.getContent()).isEmpty();
        }
    }

    @Test
    @DisplayName("search with visibility=mine should look up the viewer's id")
    void search_shouldResolveViewerForMineVisibility() {
        User viewer = new User();
        viewer.setId(UUID.randomUUID());
        when(userRepository.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));

        SearchSession session = mockPaginatedSession(List.of(), 0L);

        try (MockedStatic<org.hibernate.search.mapper.orm.Search> searchStatic =
                     Mockito.mockStatic(org.hibernate.search.mapper.orm.Search.class)) {
            searchStatic.when(() -> org.hibernate.search.mapper.orm.Search.session(entityManager))
                    .thenReturn(session);

            GameListSearchCriteria criteria = new GameListSearchCriteria(
                    null, "recent", "mine", null, null);
            Page<GameListCardDto> page = listSearchService.search(
                    criteria, PageRequest.of(0, 20), "viewer@example.com");

            assertThat(page).isNotNull();
        }
    }
}
