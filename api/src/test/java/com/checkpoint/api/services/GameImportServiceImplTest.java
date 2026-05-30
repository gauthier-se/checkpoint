package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbTimeToBeatDto;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.GameImportService.BulkImportStats;
import com.checkpoint.api.services.impl.GameImportServiceImpl;

/**
 * Unit tests for {@link GameImportServiceImpl}. The actual persistence is mocked
 * out via {@link GamePersistenceService}; these tests focus on orchestration:
 * fetching, prefetching time-to-beat once, delegation, skip/failure handling and
 * progress reporting.
 */
@ExtendWith(MockitoExtension.class)
class GameImportServiceImplTest {

    @Mock
    private IgdbApiClient igdbApiClient;

    @Mock
    private GamePersistenceService gamePersistenceService;

    @Mock
    private VideoGameRepository videoGameRepository;

    private GameImportServiceImpl gameImportService;

    @BeforeEach
    void setUp() {
        gameImportService = new GameImportServiceImpl(
                igdbApiClient, gamePersistenceService, videoGameRepository);
    }

    @Test
    @DisplayName("importTopRatedGames fetches, prefetches TTB once, and delegates per game")
    void importTopRatedGames_delegatesPerGame() {
        IgdbGameDto g1 = game(1L, "Game 1");
        IgdbGameDto g2 = game(2L, "Game 2");

        when(igdbApiClient.fetchTopRatedGames(20, 100)).thenReturn(List.of(g1, g2));
        IgdbTimeToBeatDto ttb1 = new IgdbTimeToBeatDto(1L, 3600L, 1800L, 7200L);
        when(igdbApiClient.fetchTimeToBeatForGames(anyCollection()))
                .thenReturn(Map.of(1L, ttb1));
        when(gamePersistenceService.importOne(any(), any())).thenReturn(new VideoGame());

        List<VideoGame> result = gameImportService.importTopRatedGames(20, 100);

        assertThat(result).hasSize(2);
        verify(igdbApiClient).fetchTopRatedGames(20, 100);
        verify(igdbApiClient, times(1)).fetchTimeToBeatForGames(anyCollection());
        verify(gamePersistenceService).importOne(g1, ttb1);
        verify(gamePersistenceService).importOne(g2, null);
    }

    @Test
    @DisplayName("bulkImport skips games that already exist and reports them as skipped")
    void bulkImport_skipsExisting() {
        IgdbGameDto existing = game(1L, "Existing");
        IgdbGameDto fresh = game(2L, "Fresh");

        when(igdbApiClient.fetchTimeToBeatForGames(anyCollection())).thenReturn(Map.of());
        when(videoGameRepository.existsByIgdbId(1L)).thenReturn(true);
        when(videoGameRepository.existsByIgdbId(2L)).thenReturn(false);
        when(gamePersistenceService.importOne(eq(fresh), any())).thenReturn(new VideoGame());

        BulkImportStats stats = gameImportService.bulkImport(List.of(existing, fresh));

        assertThat(stats.totalFetched()).isEqualTo(2);
        assertThat(stats.imported()).isEqualTo(1);
        assertThat(stats.skipped()).isEqualTo(1);
        assertThat(stats.failed()).isZero();
        verify(gamePersistenceService).importOne(eq(fresh), any());
        verify(gamePersistenceService, never()).importOne(eq(existing), any());
    }

    @Test
    @DisplayName("bulkImport counts a failing game without aborting the batch")
    void bulkImport_continuesOnFailure() {
        IgdbGameDto good1 = game(1L, "Good 1");
        IgdbGameDto bad = game(2L, "Bad");
        IgdbGameDto good2 = game(3L, "Good 2");

        when(igdbApiClient.fetchTimeToBeatForGames(anyCollection())).thenReturn(Map.of());
        when(videoGameRepository.existsByIgdbId(any())).thenReturn(false);
        when(gamePersistenceService.importOne(eq(good1), any())).thenReturn(new VideoGame());
        when(gamePersistenceService.importOne(eq(bad), any())).thenThrow(new RuntimeException("boom"));
        when(gamePersistenceService.importOne(eq(good2), any())).thenReturn(new VideoGame());

        BulkImportStats stats = gameImportService.bulkImport(List.of(good1, bad, good2));

        assertThat(stats.imported()).isEqualTo(2);
        assertThat(stats.failed()).isEqualTo(1);
        assertThat(stats.errors()).containsExactly("Bad");
    }

    @Test
    @DisplayName("bulkImport reports live progress through the listener")
    void bulkImport_reportsProgress() {
        IgdbGameDto fresh = game(1L, "Fresh");
        IgdbGameDto dup = game(2L, "Dup");

        when(igdbApiClient.fetchTimeToBeatForGames(anyCollection())).thenReturn(Map.of());
        when(videoGameRepository.existsByIgdbId(1L)).thenReturn(false);
        when(videoGameRepository.existsByIgdbId(2L)).thenReturn(true);
        when(gamePersistenceService.importOne(eq(fresh), any())).thenReturn(new VideoGame());

        CountingListener listener = new CountingListener();
        gameImportService.bulkImport(List.of(fresh, dup), listener);

        assertThat(listener.processed).isEqualTo(2);
        assertThat(listener.imported).isEqualTo(1);
        assertThat(listener.skipped).isEqualTo(1);
        assertThat(listener.failed).isZero();
    }

    /** Minimal IGDB game DTO with only id and name populated. */
    private IgdbGameDto game(Long id, String name) {
        return new IgdbGameDto(
                id, name, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null
        );
    }

    private static final class CountingListener implements ImportProgressListener {
        int processed;
        int imported;
        int skipped;
        int failed;

        @Override
        public void processed() {
            processed++;
        }

        @Override
        public void imported() {
            imported++;
        }

        @Override
        public void skipped() {
            skipped++;
        }

        @Override
        public void failed(String label) {
            failed++;
        }
    }
}
