package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.client.RssFeedClient;
import com.checkpoint.api.client.SteamNewsApiClient;
import com.checkpoint.api.config.RssFeedsProperties;
import com.checkpoint.api.dto.steam.SteamNewsResponseDto;
import com.checkpoint.api.entities.News;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.repositories.NewsRepository;
import com.checkpoint.api.repositories.VideoGameRepository;

/**
 * Unit tests for {@link NewsImportServiceImpl} — covers dedup, per-item / per-source
 * error isolation, lazy Steam appId backfill, and dispatch.
 */
@ExtendWith(MockitoExtension.class)
class NewsImportServiceImplTest {

    @Mock private SteamNewsApiClient steamNewsApiClient;
    @Mock private RssFeedClient rssFeedClient;
    @Mock private IgdbApiClient igdbApiClient;
    @Mock private VideoGameRepository videoGameRepository;
    @Mock private NewsRepository newsRepository;

    private RssFeedsProperties rssFeedsProperties;
    private NewsImportServiceImpl service;

    @BeforeEach
    void setUp() {
        rssFeedsProperties = new RssFeedsProperties();
        service = new NewsImportServiceImpl(
                steamNewsApiClient, rssFeedClient, igdbApiClient,
                videoGameRepository, newsRepository, rssFeedsProperties
        );
    }

    private VideoGame gameWithSteamId(Long steamAppId, Long igdbId, String title) {
        VideoGame game = new VideoGame();
        game.setId(UUID.randomUUID());
        game.setTitle(title);
        game.setIgdbId(igdbId);
        game.setSteamAppId(steamAppId);
        return game;
    }

    private SteamNewsResponseDto.NewsItem steamItem(String gid, String title) {
        return new SteamNewsResponseDto.NewsItem(
                gid, title, "https://steam/" + gid, "valve",
                "body", "Patch Notes", 1716000000L, "steam_community", 730L
        );
    }

    @Test
    @DisplayName("Steam import skips items already saved by externalId")
    void steamImport_skipsDuplicates() {
        VideoGame game = gameWithSteamId(730L, 1L, "CS2");
        when(videoGameRepository.findGamesWithAtLeastOneUserLink()).thenReturn(List.of(game));
        when(steamNewsApiClient.fetchNewsForApp(eq(730L), anyInt()))
                .thenReturn(List.of(steamItem("gid-1", "A"), steamItem("gid-2", "B")));
        when(newsRepository.existsBySourceAndExternalId(NewsSource.STEAM, "gid-1")).thenReturn(true);
        when(newsRepository.existsBySourceAndExternalId(NewsSource.STEAM, "gid-2")).thenReturn(false);

        int imported = service.importSteamNews();

        assertThat(imported).isEqualTo(1);
        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(captor.capture());
        News saved = captor.getValue();
        assertThat(saved.getExternalId()).isEqualTo("gid-2");
        assertThat(saved.getSource()).isEqualTo(NewsSource.STEAM);
        assertThat(saved.getVideoGame()).isEqualTo(game);
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Steam import: one bad item does not stop the rest of the batch")
    void steamImport_perItemIsolation() {
        VideoGame game = gameWithSteamId(730L, 1L, "CS2");
        when(videoGameRepository.findGamesWithAtLeastOneUserLink()).thenReturn(List.of(game));
        when(steamNewsApiClient.fetchNewsForApp(eq(730L), anyInt()))
                .thenReturn(List.of(steamItem("gid-1", "A"), steamItem("gid-2", "B")));
        when(newsRepository.existsBySourceAndExternalId(any(), any())).thenReturn(false);
        when(newsRepository.save(any(News.class)))
                .thenThrow(new RuntimeException("boom"))
                .thenAnswer(inv -> inv.getArgument(0));

        int imported = service.importSteamNews();

        assertThat(imported).isEqualTo(1);
        verify(newsRepository, atLeast(2)).save(any(News.class));
    }

    @Test
    @DisplayName("Steam import: backfills missing steamAppId via IGDB and uses the resolved id")
    void steamImport_backfillsSteamAppId() {
        VideoGame game = gameWithSteamId(null, 42L, "Some Game");
        when(videoGameRepository.findGamesWithAtLeastOneUserLink()).thenReturn(List.of(game));
        when(igdbApiClient.findSteamAppIdsForIgdbIds(anyCollection()))
                .thenReturn(Map.of(42L, 999L));
        when(steamNewsApiClient.fetchNewsForApp(eq(999L), anyInt()))
                .thenReturn(List.of(steamItem("gid-x", "X")));
        when(newsRepository.existsBySourceAndExternalId(any(), any())).thenReturn(false);

        int imported = service.importSteamNews();

        assertThat(imported).isEqualTo(1);
        assertThat(game.getSteamAppId()).isEqualTo(999L);
        verify(videoGameRepository).save(game);
    }

    @Test
    @DisplayName("Steam import: a game still without steamAppId after backfill is skipped silently")
    void steamImport_unresolvedGameSkipped() {
        VideoGame game = gameWithSteamId(null, 42L, "Indie Game");
        when(videoGameRepository.findGamesWithAtLeastOneUserLink()).thenReturn(List.of(game));
        when(igdbApiClient.findSteamAppIdsForIgdbIds(anyCollection())).thenReturn(Map.of());

        int imported = service.importSteamNews();

        assertThat(imported).isZero();
        verify(steamNewsApiClient, never()).fetchNewsForApp(anyLong(), anyInt());
        verify(newsRepository, never()).save(any(News.class));
    }

    @Test
    @DisplayName("Steam import: a failing Steam call for one game does not stop the others")
    void steamImport_perGameIsolation() {
        VideoGame g1 = gameWithSteamId(730L, 1L, "CS2");
        VideoGame g2 = gameWithSteamId(570L, 2L, "Dota 2");
        when(videoGameRepository.findGamesWithAtLeastOneUserLink()).thenReturn(List.of(g1, g2));
        when(steamNewsApiClient.fetchNewsForApp(eq(730L), anyInt()))
                .thenThrow(new RuntimeException("Steam 503"));
        when(steamNewsApiClient.fetchNewsForApp(eq(570L), anyInt()))
                .thenReturn(List.of(steamItem("gid-ok", "OK")));
        when(newsRepository.existsBySourceAndExternalId(any(), any())).thenReturn(false);

        int imported = service.importSteamNews();

        assertThat(imported).isEqualTo(1);
    }

    @Test
    @DisplayName("RSS import skips duplicate guids and auto-publishes new items")
    void rssImport_dedupAndAutopublish() {
        RssFeedsProperties.Feed ign = new RssFeedsProperties.Feed();
        ign.setName("IGN");
        ign.setUrl("https://example.test/ign");
        rssFeedsProperties.setFeeds(List.of(ign));

        when(rssFeedClient.fetch("IGN", "https://example.test/ign")).thenReturn(List.of(
                new RssFeedClient.RssItem("guid-a", "A", "body a", "https://a", LocalDateTime.now(), "img-a"),
                new RssFeedClient.RssItem("guid-b", "B", "body b", "https://b", LocalDateTime.now(), null)
        ));
        when(newsRepository.existsBySourceAndExternalId(NewsSource.RSS, "guid-a")).thenReturn(true);
        when(newsRepository.existsBySourceAndExternalId(NewsSource.RSS, "guid-b")).thenReturn(false);

        int imported = service.importRssFeeds();

        assertThat(imported).isEqualTo(1);
        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(captor.capture());
        News saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo(NewsSource.RSS);
        assertThat(saved.getExternalId()).isEqualTo("guid-b");
        assertThat(saved.getFeedName()).isEqualTo("IGN");
        assertThat(saved.getExternalUrl()).isEqualTo("https://b");
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("RSS import: a failing feed does not stop the next feed")
    void rssImport_perFeedIsolation() {
        RssFeedsProperties.Feed broken = new RssFeedsProperties.Feed();
        broken.setName("Broken");
        broken.setUrl("https://example.test/broken");
        RssFeedsProperties.Feed ok = new RssFeedsProperties.Feed();
        ok.setName("OK");
        ok.setUrl("https://example.test/ok");
        rssFeedsProperties.setFeeds(List.of(broken, ok));

        when(rssFeedClient.fetch("Broken", "https://example.test/broken"))
                .thenThrow(new RuntimeException("DNS"));
        when(rssFeedClient.fetch("OK", "https://example.test/ok")).thenReturn(List.of(
                new RssFeedClient.RssItem("guid-ok", "Ok", "body", "https://ok", null, null)
        ));
        when(newsRepository.existsBySourceAndExternalId(any(), any())).thenReturn(false);

        int imported = service.importRssFeeds();

        assertThat(imported).isEqualTo(1);
    }

    @Test
    @DisplayName("importFromSource dispatches and rejects MANUAL")
    void dispatch_rejectsManual() {
        assertThatThrownBy(() -> service.importFromSource(NewsSource.MANUAL))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
