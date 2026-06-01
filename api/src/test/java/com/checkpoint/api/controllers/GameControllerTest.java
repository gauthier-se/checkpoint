package com.checkpoint.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.GameDetailDto;
import com.checkpoint.api.dto.catalog.GameDetailDto.CompanyDto;
import com.checkpoint.api.dto.catalog.GameDetailDto.GenreDto;
import com.checkpoint.api.dto.catalog.GameDetailDto.PlatformDto;
import com.checkpoint.api.dto.profile.RatingDistributionEntryDto;
import com.checkpoint.api.dto.list.GameListCardDto;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.services.GameCatalogService;
import com.checkpoint.api.services.GameListService;
import com.checkpoint.api.services.GameSearchService;
import com.checkpoint.api.services.GameSimilarityService;
import com.checkpoint.api.services.GameTrendingService;

/**
 * Unit tests for {@link GameController}.
 */
@WebMvcTest(GameController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameCatalogService gameCatalogService;

    @MockitoBean
    private GameSearchService gameSearchService;

    @MockitoBean
    private GameTrendingService gameTrendingService;

    @MockitoBean
    private GameListService gameListService;

    @MockitoBean
    private GameSimilarityService gameSimilarityService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/v1/games should return paginated games")
    void getGames_shouldReturnPaginatedGames() throws Exception {
        // Given
        UUID gameId = UUID.randomUUID();
        List<GameCardDto> cards = List.of(
                new GameCardDto(gameId, "The Witcher 3", "cover.jpg", LocalDate.of(2015, 5, 19), 4.8, 1500L)
        );
        Page<GameCardDto> page = new PageImpl<>(cards);

        when(gameCatalogService.getGameCatalog(any(Pageable.class),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(page);

        // When / Then
        mockMvc.perform(get("/api/v1/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("The Witcher 3"))
                .andExpect(jsonPath("$.content[0].coverUrl").value("cover.jpg"))
                .andExpect(jsonPath("$.content[0].averageRating").value(4.8))
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/games should accept pagination parameters")
    void getGames_shouldAcceptPaginationParameters() throws Exception {
        // Given
        Page<GameCardDto> emptyPage = new PageImpl<>(List.of());
        when(gameCatalogService.getGameCatalog(any(Pageable.class),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/v1/games")
                        .param("page", "2")
                        .param("size", "50")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/games/{id} should return game details")
    void getGameById_shouldReturnGameDetails() throws Exception {
        // Given
        UUID gameId = UUID.randomUUID();
        GameDetailDto detail = new GameDetailDto(
                gameId,
                "The Witcher 3",
                "An epic RPG",
                "cover.jpg",
                "artwork.jpg",
                "dQw4w9WgXcQ",
                180000L,
                100000L,
                300000L,
                LocalDate.of(2015, 5, 19),
                4.8,
                1500L,
                List.of(new RatingDistributionEntryDto(10, 800L), new RatingDistributionEntryDto(9, 700L)),
                List.of(new GenreDto(UUID.randomUUID(), "RPG")),
                List.of(new PlatformDto(UUID.randomUUID(), "PC")),
                List.of(new CompanyDto(UUID.randomUUID(), "CD Projekt RED"))
        );

        when(gameCatalogService.getGameDetails(gameId)).thenReturn(detail);

        // When / Then
        mockMvc.perform(get("/api/v1/games/{id}", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId.toString()))
                .andExpect(jsonPath("$.title").value("The Witcher 3"))
                .andExpect(jsonPath("$.description").value("An epic RPG"))
                .andExpect(jsonPath("$.averageRating").value(4.8))
                .andExpect(jsonPath("$.genres").isArray())
                .andExpect(jsonPath("$.genres[0].name").value("RPG"))
                .andExpect(jsonPath("$.platforms[0].name").value("PC"))
                .andExpect(jsonPath("$.companies[0].name").value("CD Projekt RED"))
                .andExpect(jsonPath("$.ratingDistribution[0].score").value(10))
                .andExpect(jsonPath("$.ratingDistribution[0].count").value(800));
    }

    @Test
    @DisplayName("GET /api/v1/games/{id} should return 404 when game not found")
    void getGameById_shouldReturn404WhenNotFound() throws Exception {
        // Given
        UUID gameId = UUID.randomUUID();
        when(gameCatalogService.getGameDetails(gameId)).thenThrow(new GameNotFoundException(gameId));

        // When / Then
        mockMvc.perform(get("/api/v1/games/{id}", gameId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/games should limit page size to max 100")
    void getGames_shouldLimitPageSizeToMax() throws Exception {
        // Given
        Page<GameCardDto> emptyPage = new PageImpl<>(List.of());
        when(gameCatalogService.getGameCatalog(any(Pageable.class),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(emptyPage);

        // When / Then - requesting size 500 should be capped
        mockMvc.perform(get("/api/v1/games")
                        .param("size", "500"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/games?genre=RPG should pass genre filter to service")
    void getGames_shouldPassGenreFilter() throws Exception {
        // Given
        Page<GameCardDto> emptyPage = new PageImpl<>(List.of());
        when(gameCatalogService.getGameCatalog(any(Pageable.class),
                eq(List.of("RPG")), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/v1/games")
                        .param("genre", "RPG"))
                .andExpect(status().isOk());

        verify(gameCatalogService).getGameCatalog(any(Pageable.class),
                eq(List.of("RPG")), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("GET /api/v1/games?genre=RPG&genre=Action should pass multiple genre filters to service")
    void getGames_shouldPassMultipleGenreFilters() throws Exception {
        // Given
        Page<GameCardDto> emptyPage = new PageImpl<>(List.of());
        when(gameCatalogService.getGameCatalog(any(Pageable.class),
                eq(List.of("RPG", "Action")), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/v1/games")
                        .param("genre", "RPG")
                        .param("genre", "Action"))
                .andExpect(status().isOk());

        verify(gameCatalogService).getGameCatalog(any(Pageable.class),
                eq(List.of("RPG", "Action")), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("GET /api/v1/games?platform=PC should pass platform filter to service")
    void getGames_shouldPassPlatformFilter() throws Exception {
        // Given
        Page<GameCardDto> emptyPage = new PageImpl<>(List.of());
        when(gameCatalogService.getGameCatalog(any(Pageable.class),
                isNull(), eq(List.of("PC")), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/v1/games")
                        .param("platform", "PC"))
                .andExpect(status().isOk());

        verify(gameCatalogService).getGameCatalog(any(Pageable.class),
                isNull(), eq(List.of("PC")), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("GET /api/v1/games?yearMin=2020&yearMax=2023 should pass year range filters to service")
    void getGames_shouldPassYearRangeFilters() throws Exception {
        // Given
        Page<GameCardDto> emptyPage = new PageImpl<>(List.of());
        when(gameCatalogService.getGameCatalog(any(Pageable.class),
                isNull(), isNull(), eq(2020), eq(2023), isNull(), isNull()))
                .thenReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/v1/games")
                        .param("yearMin", "2020")
                        .param("yearMax", "2023"))
                .andExpect(status().isOk());

        verify(gameCatalogService).getGameCatalog(any(Pageable.class),
                isNull(), isNull(), eq(2020), eq(2023), isNull(), isNull());
    }

    @Test
    @DisplayName("GET /api/v1/games?ratingMin=4.0&ratingMax=5.0 should pass rating range filters to service")
    void getGames_shouldPassRatingRangeFilters() throws Exception {
        // Given
        Page<GameCardDto> emptyPage = new PageImpl<>(List.of());
        when(gameCatalogService.getGameCatalog(any(Pageable.class),
                isNull(), isNull(), isNull(), isNull(), eq(4.0), eq(5.0)))
                .thenReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/v1/games")
                        .param("ratingMin", "4.0")
                        .param("ratingMax", "5.0"))
                .andExpect(status().isOk());

        verify(gameCatalogService).getGameCatalog(any(Pageable.class),
                isNull(), isNull(), isNull(), isNull(), eq(4.0), eq(5.0));
    }

    @Test
    @DisplayName("GET /api/v1/games should support combining multiple filters")
    void getGames_shouldSupportCombinedFilters() throws Exception {
        // Given
        UUID gameId = UUID.randomUUID();
        List<GameCardDto> cards = List.of(
                new GameCardDto(gameId, "Elden Ring", "cover.jpg", LocalDate.of(2022, 2, 25), 4.9, 2000L)
        );
        Page<GameCardDto> page = new PageImpl<>(cards);

        when(gameCatalogService.getGameCatalog(any(Pageable.class),
                eq(List.of("RPG")), eq(List.of("PC")), eq(2020), isNull(), eq(4.0), isNull()))
                .thenReturn(page);

        // When / Then
        mockMvc.perform(get("/api/v1/games")
                        .param("genre", "RPG")
                        .param("platform", "PC")
                        .param("yearMin", "2020")
                        .param("ratingMin", "4.0")
                        .param("sort", "rating,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Elden Ring"));
    }

    @Test
    @DisplayName("GET /api/v1/games/search should return search results")
    void searchGames_shouldReturnResults() throws Exception {
        // Given
        UUID gameId = UUID.randomUUID();
        List<GameCardDto> results = List.of(
                new GameCardDto(gameId, "The Witcher 3", "cover.jpg", LocalDate.of(2015, 5, 19), 4.8, 1500L)
        );

        when(gameSearchService.searchGames(eq("Witchr"), isNull(), isNull())).thenReturn(results);

        // When / Then
        mockMvc.perform(get("/api/v1/games/search")
                        .param("q", "Witchr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("The Witcher 3"))
                .andExpect(jsonPath("$[0].averageRating").value(4.8));
    }

    @Test
    @DisplayName("GET /api/v1/games/search should support genre and platform filters")
    void searchGames_shouldSupportFilters() throws Exception {
        // Given
        UUID gameId = UUID.randomUUID();
        List<GameCardDto> results = List.of(
                new GameCardDto(gameId, "The Witcher 3", "cover.jpg", LocalDate.of(2015, 5, 19), 4.8, 1500L)
        );

        when(gameSearchService.searchGames(eq("Witcher"), eq("RPG"), eq("PC"))).thenReturn(results);

        // When / Then
        mockMvc.perform(get("/api/v1/games/search")
                        .param("q", "Witcher")
                        .param("genre", "RPG")
                        .param("platform", "PC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("The Witcher 3"));
    }

    @Test
    @DisplayName("GET /api/v1/games/search should return empty list when no matches")
    void searchGames_shouldReturnEmptyListWhenNoMatches() throws Exception {
        // Given
        when(gameSearchService.searchGames(eq("nonexistent"), isNull(), isNull())).thenReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/v1/games/search")
                        .param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/games/search should return 400 when query parameter is missing")
    void searchGames_shouldReturn400WhenQueryMissing() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/games/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/games/trending should return trending games")
    void getTrendingGames_shouldReturnTrendingGames() throws Exception {
        // Given
        UUID gameId = UUID.randomUUID();
        List<GameCardDto> trending = List.of(
                new GameCardDto(gameId, "Elden Ring", "cover.jpg", LocalDate.of(2022, 2, 25), 4.9, 2000L)
        );

        when(gameTrendingService.getTrendingGames(7)).thenReturn(trending);

        // When / Then
        mockMvc.perform(get("/api/v1/games/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Elden Ring"))
                .andExpect(jsonPath("$[0].averageRating").value(4.9))
                .andExpect(jsonPath("$[0].ratingCount").value(2000));
    }

    @Test
    @DisplayName("GET /api/v1/games/trending should cap size to maximum 20")
    void getTrendingGames_shouldCapSizeToMaximum() throws Exception {
        // Given
        when(gameTrendingService.getTrendingGames(anyInt())).thenReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/v1/games/trending")
                        .param("size", "50"))
                .andExpect(status().isOk());

        verify(gameTrendingService).getTrendingGames(20);
    }

    @Test
    @DisplayName("GET /api/v1/games/trending should accept custom size parameter")
    void getTrendingGames_shouldAcceptCustomSize() throws Exception {
        // Given
        when(gameTrendingService.getTrendingGames(anyInt())).thenReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/v1/games/trending")
                        .param("size", "3"))
                .andExpect(status().isOk());

        verify(gameTrendingService).getTrendingGames(3);
    }

    @Test
    @DisplayName("GET /api/v1/games/most-backlogged should return games ordered by backlog count")
    void getMostBackloggedGames_shouldReturnGames() throws Exception {
        UUID gameId = UUID.randomUUID();
        List<GameCardDto> games = List.of(
                new GameCardDto(gameId, "Persona 5", "cover.jpg", LocalDate.of(2016, 9, 15), 4.7, 800L)
        );
        when(gameCatalogService.getMostBackloggedGames(7)).thenReturn(games);

        mockMvc.perform(get("/api/v1/games/most-backlogged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Persona 5"))
                .andExpect(jsonPath("$[0].averageRating").value(4.7));
    }

    @Test
    @DisplayName("GET /api/v1/games/most-backlogged should return empty list when no games")
    void getMostBackloggedGames_shouldReturnEmptyListWhenNone() throws Exception {
        when(gameCatalogService.getMostBackloggedGames(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/games/most-backlogged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/games/most-backlogged should accept custom size parameter")
    void getMostBackloggedGames_shouldAcceptCustomSize() throws Exception {
        when(gameCatalogService.getMostBackloggedGames(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/games/most-backlogged").param("size", "5"))
                .andExpect(status().isOk());

        verify(gameCatalogService).getMostBackloggedGames(5);
    }

    @Test
    @DisplayName("GET /api/v1/games/most-backlogged should cap size to maximum 20")
    void getMostBackloggedGames_shouldCapSizeToMaximum() throws Exception {
        when(gameCatalogService.getMostBackloggedGames(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/games/most-backlogged").param("size", "100"))
                .andExpect(status().isOk());

        verify(gameCatalogService).getMostBackloggedGames(20);
    }

    @Test
    @DisplayName("GET /api/v1/games/most-wishlisted should return games ordered by wishlist count")
    void getMostWishlistedGames_shouldReturnGames() throws Exception {
        UUID gameId = UUID.randomUUID();
        List<GameCardDto> games = List.of(
                new GameCardDto(gameId, "Silksong", "cover.jpg", LocalDate.of(2026, 1, 1), null, 0L)
        );
        when(gameCatalogService.getMostWishlistedGames(7)).thenReturn(games);

        mockMvc.perform(get("/api/v1/games/most-wishlisted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Silksong"));
    }

    @Test
    @DisplayName("GET /api/v1/games/most-wishlisted should return empty list when no games")
    void getMostWishlistedGames_shouldReturnEmptyListWhenNone() throws Exception {
        when(gameCatalogService.getMostWishlistedGames(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/games/most-wishlisted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/games/most-wishlisted should accept custom size parameter")
    void getMostWishlistedGames_shouldAcceptCustomSize() throws Exception {
        when(gameCatalogService.getMostWishlistedGames(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/games/most-wishlisted").param("size", "10"))
                .andExpect(status().isOk());

        verify(gameCatalogService).getMostWishlistedGames(10);
    }

    @Test
    @DisplayName("GET /api/v1/games/most-wishlisted should cap size to maximum 20")
    void getMostWishlistedGames_shouldCapSizeToMaximum() throws Exception {
        when(gameCatalogService.getMostWishlistedGames(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/games/most-wishlisted").param("size", "50"))
                .andExpect(status().isOk());

        verify(gameCatalogService).getMostWishlistedGames(20);
    }

    @Test
    @DisplayName("GET /api/v1/games/{gameId}/lists should return paginated lists containing the game")
    void getListsContainingGame_shouldReturnPaginatedLists() throws Exception {
        UUID gameId = UUID.randomUUID();
        GameListCardDto card = new GameListCardDto(
                UUID.randomUUID(), "Best Indies", null, false,
                12, 42L, 0L, "curator", null, List.of("cover1.jpg"),
                LocalDateTime.now());
        Page<GameListCardDto> page = new PageImpl<>(List.of(card), PageRequest.of(0, 6), 1);

        when(gameListService.findListsContainingGame(eq(gameId), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/games/{gameId}/lists", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Best Indies"))
                .andExpect(jsonPath("$.content[0].likesCount").value(42))
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/games/{gameId}/lists should pass null viewerEmail when anonymous")
    void getListsContainingGame_shouldPassNullViewerEmailWhenAnonymous() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameListService.findListsContainingGame(eq(gameId), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 6), 0));

        mockMvc.perform(get("/api/v1/games/{gameId}/lists", gameId))
                .andExpect(status().isOk());

        verify(gameListService).findListsContainingGame(eq(gameId), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/games/{gameId}/lists should pass viewer email when authenticated")
    @WithMockUser(username = "user@example.com")
    void getListsContainingGame_shouldPassViewerEmailWhenAuthenticated() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameListService.findListsContainingGame(eq(gameId), eq("user@example.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 6), 0));

        mockMvc.perform(get("/api/v1/games/{gameId}/lists", gameId))
                .andExpect(status().isOk());

        verify(gameListService).findListsContainingGame(eq(gameId), eq("user@example.com"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/games/{gameId}/lists should clamp size to maximum 50")
    void getListsContainingGame_shouldClampSizeToMaximum() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameListService.findListsContainingGame(eq(gameId), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api/v1/games/{gameId}/lists", gameId).param("size", "999"))
                .andExpect(status().isOk());

        verify(gameListService).findListsContainingGame(
                eq(gameId),
                isNull(),
                argThat((Pageable p) -> p.getPageSize() == 50));
    }

    @Test
    @DisplayName("GET /api/v1/games/{gameId}/lists should default to size=6")
    void getListsContainingGame_shouldDefaultToSize6() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameListService.findListsContainingGame(eq(gameId), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 6), 0));

        mockMvc.perform(get("/api/v1/games/{gameId}/lists", gameId))
                .andExpect(status().isOk());

        verify(gameListService).findListsContainingGame(
                eq(gameId),
                isNull(),
                argThat((Pageable p) -> p.getPageSize() == 6 && p.getPageNumber() == 0));
    }

    @Test
    @DisplayName("GET /api/v1/games/{gameId}/similar should return similar games")
    void getSimilarGames_shouldReturnSimilarGames() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID similarId = UUID.randomUUID();
        List<GameCardDto> similar = List.of(
                new GameCardDto(similarId, "Similar Game", "cover.jpg", LocalDate.of(2021, 3, 1), 4.5, 200L)
        );
        when(gameSimilarityService.getSimilarGames(eq(gameId), anyInt())).thenReturn(similar);

        mockMvc.perform(get("/api/v1/games/{gameId}/similar", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(similarId.toString()))
                .andExpect(jsonPath("$[0].title").value("Similar Game"));
    }

    @Test
    @DisplayName("GET /api/v1/games/{gameId}/similar should return empty list when no similar games")
    void getSimilarGames_shouldReturnEmptyList() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameSimilarityService.getSimilarGames(eq(gameId), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/games/{gameId}/similar", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/games/{gameId}/similar should clamp size to maximum 30")
    void getSimilarGames_shouldClampSizeToMaximum() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameSimilarityService.getSimilarGames(eq(gameId), eq(30))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/games/{gameId}/similar", gameId).param("size", "999"))
                .andExpect(status().isOk());

        verify(gameSimilarityService).getSimilarGames(eq(gameId), eq(30));
    }
}
