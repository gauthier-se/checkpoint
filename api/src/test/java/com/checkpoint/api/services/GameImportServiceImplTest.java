package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.dto.igdb.IgdbCompanyDto;
import com.checkpoint.api.dto.igdb.IgdbCoverDto;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbGenreDto;
import com.checkpoint.api.dto.igdb.IgdbInvolvedCompanyDto;
import com.checkpoint.api.dto.igdb.IgdbPlatformDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.mapper.GameMapper;
import com.checkpoint.api.repositories.CompanyRepository;
import com.checkpoint.api.repositories.GenreRepository;
import com.checkpoint.api.repositories.PlatformRepository;
import com.checkpoint.api.repositories.VideoGameRepository;

/**
 * Unit tests for {@link GameImportServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class GameImportServiceImplTest {

    @Mock
    private IgdbApiClient igdbApiClient;

    @Mock
    private GameMapper gameMapper;

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private PlatformRepository platformRepository;

    @Mock
    private CompanyRepository companyRepository;

    private GameImportServiceImpl gameImportService;

    @BeforeEach
    void setUp() {
        gameImportService = new GameImportServiceImpl(
                igdbApiClient,
                gameMapper,
                videoGameRepository,
                genreRepository,
                platformRepository,
                companyRepository
        );
    }

    @Test
    @DisplayName("importRecentlyReleasedGames should create new games")
    void importRecentlyReleasedGames_shouldCreateNewGames() {
        // Given
        IgdbGameDto dto = createSampleGameDto(1942L, "The Witcher 3");
        List<IgdbGameDto> games = List.of(dto);

        VideoGame newEntity = new VideoGame();
        newEntity.setIgdbId(1942L);
        newEntity.setTitle("The Witcher 3");

        when(igdbApiClient.fetchRecentlyReleasedGames(10)).thenReturn(games);
        when(videoGameRepository.findByIgdbId(1942L)).thenReturn(Optional.empty());
        when(gameMapper.toEntity(dto)).thenReturn(newEntity);
        when(genreRepository.findByNameIgnoreCase("RPG")).thenReturn(Optional.of(new Genre("RPG")));
        when(platformRepository.findByNameIgnoreCase("PC")).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase("CD Projekt RED")).thenReturn(Optional.of(new Company("CD Projekt RED")));
        when(videoGameRepository.save(any(VideoGame.class))).thenReturn(newEntity);

        // When
        List<VideoGame> result = gameImportService.importRecentlyReleasedGames(10);

        // Then
        assertThat(result).hasSize(1);
        verify(gameMapper).toEntity(dto);
        verify(gameMapper, never()).updateEntity(any(), any());
        verify(videoGameRepository).save(any(VideoGame.class));
    }

    @Test
    @DisplayName("importRecentlyReleasedGames should update existing games (duplicate handling)")
    void importRecentlyReleasedGames_shouldUpdateExistingGames() {
        // Given
        IgdbGameDto dto = createSampleGameDto(1942L, "The Witcher 3: Updated");
        List<IgdbGameDto> games = List.of(dto);

        VideoGame existingEntity = new VideoGame();
        existingEntity.setId(UUID.randomUUID());
        existingEntity.setIgdbId(1942L);
        existingEntity.setTitle("The Witcher 3");

        when(igdbApiClient.fetchRecentlyReleasedGames(10)).thenReturn(games);
        when(videoGameRepository.findByIgdbId(1942L)).thenReturn(Optional.of(existingEntity));
        when(genreRepository.findByNameIgnoreCase("RPG")).thenReturn(Optional.of(new Genre("RPG")));
        when(platformRepository.findByNameIgnoreCase("PC")).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase("CD Projekt RED")).thenReturn(Optional.of(new Company("CD Projekt RED")));
        when(videoGameRepository.save(any(VideoGame.class))).thenReturn(existingEntity);

        // When
        List<VideoGame> result = gameImportService.importRecentlyReleasedGames(10);

        // Then
        assertThat(result).hasSize(1);
        verify(gameMapper, never()).toEntity(any());
        verify(gameMapper).updateEntity(dto, existingEntity);
        verify(videoGameRepository).save(existingEntity);
    }

    @Test
    @DisplayName("importRecentlyReleasedGames should create genres if not found")
    void importRecentlyReleasedGames_shouldCreateGenresIfNotFound() {
        // Given
        IgdbGameDto dto = createSampleGameDto(1L, "Test Game");
        List<IgdbGameDto> games = List.of(dto);

        VideoGame newEntity = new VideoGame();
        Genre newGenre = new Genre("RPG");

        when(igdbApiClient.fetchRecentlyReleasedGames(10)).thenReturn(games);
        when(videoGameRepository.findByIgdbId(1L)).thenReturn(Optional.empty());
        when(gameMapper.toEntity(dto)).thenReturn(newEntity);
        when(genreRepository.findByNameIgnoreCase("RPG")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenReturn(newGenre);
        when(platformRepository.findByNameIgnoreCase("PC")).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase("CD Projekt RED")).thenReturn(Optional.of(new Company("CD Projekt RED")));
        when(videoGameRepository.save(any(VideoGame.class))).thenReturn(newEntity);

        // When
        gameImportService.importRecentlyReleasedGames(10);

        // Then
        ArgumentCaptor<Genre> genreCaptor = ArgumentCaptor.forClass(Genre.class);
        verify(genreRepository).save(genreCaptor.capture());
        assertThat(genreCaptor.getValue().getName()).isEqualTo("RPG");
    }

    @Test
    @DisplayName("searchAndImportGames should use search API")
    void searchAndImportGames_shouldUseSearchApi() {
        // Given
        IgdbGameDto dto = createSampleGameDto(1L, "Test Game");
        List<IgdbGameDto> games = List.of(dto);
        VideoGame newEntity = new VideoGame();

        when(igdbApiClient.searchGames("witcher", 5)).thenReturn(games);
        when(videoGameRepository.findByIgdbId(1L)).thenReturn(Optional.empty());
        when(gameMapper.toEntity(dto)).thenReturn(newEntity);
        when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Genre("RPG")));
        when(platformRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Company("Company")));
        when(videoGameRepository.save(any())).thenReturn(newEntity);

        // When
        List<VideoGame> result = gameImportService.searchAndImportGames("witcher", 5);

        // Then
        assertThat(result).hasSize(1);
        verify(igdbApiClient).searchGames("witcher", 5);
    }

    @Test
    @DisplayName("importGamesByIds should use fetch by IDs API")
    void importGamesByIds_shouldUseFetchByIdsApi() {
        // Given
        List<Long> ids = List.of(1L, 2L);
        IgdbGameDto dto1 = createSampleGameDto(1L, "Game 1");
        IgdbGameDto dto2 = createSampleGameDto(2L, "Game 2");
        List<IgdbGameDto> games = List.of(dto1, dto2);

        when(igdbApiClient.fetchGamesByIds(ids)).thenReturn(games);
        when(videoGameRepository.findByIgdbId(any())).thenReturn(Optional.empty());
        when(gameMapper.toEntity(any())).thenReturn(new VideoGame());
        when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Genre("RPG")));
        when(platformRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Company("Company")));
        when(videoGameRepository.save(any())).thenReturn(new VideoGame());

        // When
        List<VideoGame> result = gameImportService.importGamesByIds(ids);

        // Then
        assertThat(result).hasSize(2);
        verify(igdbApiClient).fetchGamesByIds(ids);
        verify(videoGameRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("importTopRatedGames should use top rated API")
    void importTopRatedGames_shouldUseTopRatedApi() {
        // Given
        IgdbGameDto dto = createSampleGameDto(1L, "Top Game");
        List<IgdbGameDto> games = List.of(dto);
        VideoGame newEntity = new VideoGame();

        when(igdbApiClient.fetchTopRatedGames(20, 100)).thenReturn(games);
        when(videoGameRepository.findByIgdbId(1L)).thenReturn(Optional.empty());
        when(gameMapper.toEntity(dto)).thenReturn(newEntity);
        when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Genre("RPG")));
        when(platformRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Company("Company")));
        when(videoGameRepository.save(any())).thenReturn(newEntity);

        // When
        List<VideoGame> result = gameImportService.importTopRatedGames(20, 100);

        // Then
        assertThat(result).hasSize(1);
        verify(igdbApiClient).fetchTopRatedGames(20, 100);
    }

    /**
     * Creates a sample IgdbGameDto for testing.
     */
    private IgdbGameDto createSampleGameDto(Long id, String name) {
        IgdbCoverDto cover = new IgdbCoverDto(
                1L, "co1wyy", "//images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg",
                264, 374, false, false
        );

        IgdbGenreDto genre = new IgdbGenreDto(12L, "RPG", "rpg", "url");

        IgdbPlatformDto platform = new IgdbPlatformDto(
                6L, "PC", "win", "PC", null, null, null, null, "url"
        );

        IgdbCompanyDto company = new IgdbCompanyDto(
                908L, "CD Projekt RED", "cd-projekt-red", "Polish developer",
                null, null, "url", 616
        );

        IgdbInvolvedCompanyDto involvedCompany = new IgdbInvolvedCompanyDto(
                1L, company, true, true, false, false
        );

        return new IgdbGameDto(
                id,
                name,
                name.toLowerCase().replace(" ", "-"),
                "Summary of " + name,
                null,
                1431993600L,
                92.5, 1250, 93.8, 85, 93.15, 1335,
                cover,
                List.of(genre),
                List.of(platform),
                List.of(involvedCompany),
                null,  // screenshots
                null,  // similarGames
                null,  // gameModes
                null,  // themes
                null,  // playerPerspectives
                null,  // parentGame
                null,  // dlcs
                null,  // expansions
                null,  // versionTitle
                "url"
        );
    }
}
