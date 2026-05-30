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

import com.checkpoint.api.dto.igdb.IgdbCompanyDto;
import com.checkpoint.api.dto.igdb.IgdbCoverDto;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbGenreDto;
import com.checkpoint.api.dto.igdb.IgdbInvolvedCompanyDto;
import com.checkpoint.api.dto.igdb.IgdbPlatformDto;
import com.checkpoint.api.dto.igdb.IgdbTimeToBeatDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.mapper.GameMapper;
import com.checkpoint.api.repositories.CompanyRepository;
import com.checkpoint.api.repositories.GenreRepository;
import com.checkpoint.api.repositories.PlatformRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.impl.GamePersistenceServiceImpl;

/**
 * Unit tests for {@link GamePersistenceServiceImpl} — the per-game upsert that
 * resolves relationships and applies the pre-fetched time-to-beat data.
 */
@ExtendWith(MockitoExtension.class)
class GamePersistenceServiceImplTest {

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

    private GamePersistenceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GamePersistenceServiceImpl(
                gameMapper, videoGameRepository, genreRepository, platformRepository, companyRepository);
    }

    @Test
    @DisplayName("importOne creates a new game and resolves its relationships")
    void importOne_createsNewGame() {
        IgdbGameDto dto = sampleGameDto(1942L, "The Witcher 3");
        VideoGame entity = new VideoGame();

        when(videoGameRepository.findByIgdbId(1942L)).thenReturn(Optional.empty());
        when(gameMapper.toEntity(dto)).thenReturn(entity);
        when(genreRepository.findByNameIgnoreCase("RPG")).thenReturn(Optional.of(new Genre("RPG")));
        when(platformRepository.findByNameIgnoreCase("PC")).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase("CD Projekt RED")).thenReturn(Optional.of(new Company("CD Projekt RED")));
        when(videoGameRepository.save(any(VideoGame.class))).thenReturn(entity);

        service.importOne(dto, null);

        verify(gameMapper).toEntity(dto);
        verify(gameMapper, never()).updateEntity(any(), any());
        // saved once to generate the id, once after relationships are wired
        verify(videoGameRepository, times(2)).save(any(VideoGame.class));
    }

    @Test
    @DisplayName("importOne updates an existing game (upsert by igdbId)")
    void importOne_updatesExistingGame() {
        IgdbGameDto dto = sampleGameDto(1942L, "The Witcher 3: Updated");
        VideoGame existing = new VideoGame();
        existing.setId(UUID.randomUUID());
        existing.setIgdbId(1942L);

        when(videoGameRepository.findByIgdbId(1942L)).thenReturn(Optional.of(existing));
        when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Genre("RPG")));
        when(platformRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Company("Company")));
        when(videoGameRepository.save(any(VideoGame.class))).thenReturn(existing);

        service.importOne(dto, null);

        verify(gameMapper, never()).toEntity(any());
        verify(gameMapper).updateEntity(dto, existing);
    }

    @Test
    @DisplayName("importOne creates genres that do not yet exist")
    void importOne_createsMissingGenre() {
        IgdbGameDto dto = sampleGameDto(1L, "Test Game");
        VideoGame entity = new VideoGame();

        when(videoGameRepository.findByIgdbId(1L)).thenReturn(Optional.empty());
        when(gameMapper.toEntity(dto)).thenReturn(entity);
        when(genreRepository.findByNameIgnoreCase("RPG")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenReturn(new Genre("RPG"));
        when(platformRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Company("Company")));
        when(videoGameRepository.save(any(VideoGame.class))).thenReturn(entity);

        service.importOne(dto, null);

        ArgumentCaptor<Genre> captor = ArgumentCaptor.forClass(Genre.class);
        verify(genreRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("RPG");
    }

    @Test
    @DisplayName("importOne applies the supplied time-to-beat data")
    void importOne_appliesTimeToBeat() {
        IgdbGameDto dto = sampleGameDto(1L, "Test Game");
        VideoGame entity = new VideoGame();

        when(videoGameRepository.findByIgdbId(1L)).thenReturn(Optional.empty());
        when(gameMapper.toEntity(dto)).thenReturn(entity);
        when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Genre("RPG")));
        when(platformRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Company("Company")));
        when(videoGameRepository.save(any(VideoGame.class))).thenReturn(entity);

        service.importOne(dto, new IgdbTimeToBeatDto(1L, 3600L, 1800L, 7200L));

        assertThat(entity.getTimeToBeatNormally()).isEqualTo(3600L);
        assertThat(entity.getTimeToBeatHastily()).isEqualTo(1800L);
        assertThat(entity.getTimeToBeatCompletely()).isEqualTo(7200L);
    }

    @Test
    @DisplayName("importOne leaves time-to-beat null when none is supplied")
    void importOne_noTimeToBeat() {
        IgdbGameDto dto = sampleGameDto(1L, "Test Game");
        VideoGame entity = new VideoGame();

        when(videoGameRepository.findByIgdbId(1L)).thenReturn(Optional.empty());
        when(gameMapper.toEntity(dto)).thenReturn(entity);
        when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Genre("RPG")));
        when(platformRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Platform("PC")));
        when(companyRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Company("Company")));
        when(videoGameRepository.save(any(VideoGame.class))).thenReturn(entity);

        service.importOne(dto, null);

        assertThat(entity.getTimeToBeatNormally()).isNull();
        assertThat(entity.getTimeToBeatHastily()).isNull();
        assertThat(entity.getTimeToBeatCompletely()).isNull();
    }

    private IgdbGameDto sampleGameDto(Long id, String name) {
        IgdbCoverDto cover = new IgdbCoverDto(
                1L, "co1wyy", "//images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg",
                264, 374, false, false);
        IgdbGenreDto genre = new IgdbGenreDto(12L, "RPG", "rpg", "url");
        IgdbPlatformDto platform = new IgdbPlatformDto(
                6L, "PC", "win", "PC", null, null, null, null, "url");
        IgdbCompanyDto company = new IgdbCompanyDto(
                908L, "CD Projekt RED", "cd-projekt-red", "Polish developer",
                null, null, "url", 616);
        IgdbInvolvedCompanyDto involved = new IgdbInvolvedCompanyDto(1L, company, true, true, false, false);

        return new IgdbGameDto(
                id, name, name.toLowerCase().replace(" ", "-"),
                "Summary of " + name, null, 1431993600L,
                92.5, 1250, 93.8, 85, 93.15, 1335,
                cover, List.of(genre), List.of(platform), List.of(involved),
                null, null, null, null, null, null, null, null, null, null, null, "url");
    }
}
