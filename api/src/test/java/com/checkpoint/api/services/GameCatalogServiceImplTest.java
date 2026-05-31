package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.GameDetailDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.impl.GameCatalogServiceImpl;

/**
 * Unit tests for {@link GameCatalogServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class GameCatalogServiceImplTest {

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private BacklogRepository backlogRepository;

    @Mock
    private WishRepository wishRepository;

    @Mock
    private com.checkpoint.api.repositories.RateRepository rateRepository;

    private GameCatalogServiceImpl gameCatalogService;

    @BeforeEach
    void setUp() {
        gameCatalogService = new GameCatalogServiceImpl(
                videoGameRepository, backlogRepository, wishRepository, rateRepository);
    }

    @Test
    @DisplayName("getGameCatalog should return paginated game cards without filters")
    void getGameCatalog_shouldReturnPaginatedGameCards() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "releaseDate"));

        List<GameCardDto> cards = List.of(
                new GameCardDto(UUID.randomUUID(), "Game 1", "cover1.jpg", LocalDate.of(2025, 1, 15), 4.5, 100L),
                new GameCardDto(UUID.randomUUID(), "Game 2", "cover2.jpg", LocalDate.of(2025, 1, 10), 4.0, 50L)
        );
        Page<GameCardDto> expectedPage = new PageImpl<>(cards, pageable, 2);

        when(videoGameRepository.findAllAsGameCardsWithFilters(pageable,
                null, null, null, null, null, null))
                .thenReturn(expectedPage);

        // When
        Page<GameCardDto> result = gameCatalogService.getGameCatalog(
                pageable, null, null, null, null, null, null);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("Game 1");
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(videoGameRepository).findAllAsGameCardsWithFilters(pageable,
                null, null, null, null, null, null);
    }

    @Test
    @DisplayName("getGameCatalog should pass filter parameters to repository")
    void getGameCatalog_shouldPassFiltersToRepository() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "releaseDate"));
        Page<GameCardDto> expectedPage = new PageImpl<>(List.of(), pageable, 0);

        when(videoGameRepository.findAllAsGameCardsWithFilters(pageable,
                List.of("RPG"), List.of("PC"), 2020, 2023, 4.0, 5.0))
                .thenReturn(expectedPage);

        // When
        Page<GameCardDto> result = gameCatalogService.getGameCatalog(
                pageable, List.of("RPG"), List.of("PC"), 2020, 2023, 4.0, 5.0);

        // Then
        assertThat(result.getContent()).isEmpty();
        verify(videoGameRepository).findAllAsGameCardsWithFilters(pageable,
                List.of("RPG"), List.of("PC"), 2020, 2023, 4.0, 5.0);
    }

    @Test
    @DisplayName("getGameDetails should return full game details")
    void getGameDetails_shouldReturnFullGameDetails() {
        // Given
        UUID gameId = UUID.randomUUID();

        VideoGame game = new VideoGame();
        game.setId(gameId);
        game.setTitle("The Witcher 3");
        game.setDescription("An epic RPG adventure");
        game.setCoverUrl("witcher3-cover.jpg");
        game.setReleaseDate(LocalDate.of(2015, 5, 19));

        Genre genre = new Genre("RPG");
        genre.setId(UUID.randomUUID());
        game.setGenres(Set.of(genre));

        Platform platform = new Platform("PC");
        platform.setId(UUID.randomUUID());
        game.setPlatforms(Set.of(platform));

        Company company = new Company("CD Projekt RED");
        company.setId(UUID.randomUUID());
        game.setCompanies(Set.of(company));

        when(videoGameRepository.findByIdWithRelationships(gameId)).thenReturn(Optional.of(game));
        game.setAverageRating(4.8);
        when(videoGameRepository.countRatings(gameId)).thenReturn(1500L);
        when(rateRepository.findDistributionByVideoGameId(gameId)).thenReturn(
                List.of(new com.checkpoint.api.dto.profile.RatingDistributionEntryDto(10, 800L),
                        new com.checkpoint.api.dto.profile.RatingDistributionEntryDto(9, 700L)));

        // When
        GameDetailDto result = gameCatalogService.getGameDetails(gameId);

        // Then
        assertThat(result.id()).isEqualTo(gameId);
        assertThat(result.title()).isEqualTo("The Witcher 3");
        assertThat(result.description()).isEqualTo("An epic RPG adventure");
        assertThat(result.averageRating()).isEqualTo(4.8);
        assertThat(result.ratingCount()).isEqualTo(1500L);
        assertThat(result.ratingDistribution()).hasSize(2);
        assertThat(result.ratingDistribution().get(0).score()).isEqualTo(10);
        assertThat(result.ratingDistribution().get(0).count()).isEqualTo(800L);
        assertThat(result.genres()).hasSize(1);
        assertThat(result.genres().get(0).name()).isEqualTo("RPG");
        assertThat(result.platforms()).hasSize(1);
        assertThat(result.companies()).hasSize(1);
    }

    @Test
    @DisplayName("getGameDetails should throw GameNotFoundException when game not found")
    void getGameDetails_shouldThrowWhenGameNotFound() {
        // Given
        UUID gameId = UUID.randomUUID();
        when(videoGameRepository.findByIdWithRelationships(gameId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> gameCatalogService.getGameDetails(gameId))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining(gameId.toString());
    }

    @Test
    @DisplayName("getGameDetails should handle null rating gracefully")
    void getGameDetails_shouldHandleNullRating() {
        // Given
        UUID gameId = UUID.randomUUID();

        VideoGame game = new VideoGame();
        game.setId(gameId);
        game.setTitle("New Game");
        game.setGenres(Set.of());
        game.setPlatforms(Set.of());
        game.setCompanies(Set.of());

        when(videoGameRepository.findByIdWithRelationships(gameId)).thenReturn(Optional.of(game));
        game.setAverageRating(null);
        when(videoGameRepository.countRatings(gameId)).thenReturn(0L);

        // When
        GameDetailDto result = gameCatalogService.getGameDetails(gameId);

        // Then
        assertThat(result.averageRating()).isNull();
        assertThat(result.ratingCount()).isEqualTo(0L);
    }
}
