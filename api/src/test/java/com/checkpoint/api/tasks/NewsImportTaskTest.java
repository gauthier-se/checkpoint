package com.checkpoint.api.tasks;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.services.NewsImportService;
import com.checkpoint.api.services.NewsImportSettingsService;

/**
 * Unit tests for {@link NewsImportTask}: the scheduled passes must respect the
 * per-source pause switch, and a failing pass must not escape the task.
 */
@ExtendWith(MockitoExtension.class)
class NewsImportTaskTest {

    @Mock private NewsImportService newsImportService;
    @Mock private NewsImportSettingsService newsImportSettingsService;

    @InjectMocks private NewsImportTask task;

    @Test
    @DisplayName("A paused Steam source turns the scheduled pass into a no-op")
    void steamPass_skippedWhenPaused() {
        when(newsImportSettingsService.isScheduledPassEnabled(NewsSource.STEAM)).thenReturn(false);

        task.runSteamPass();

        verify(newsImportService, never()).importSteamNews();
    }

    @Test
    @DisplayName("A paused RSS source turns the scheduled pass into a no-op")
    void rssPass_skippedWhenPaused() {
        when(newsImportSettingsService.isScheduledPassEnabled(NewsSource.RSS)).thenReturn(false);

        task.runRssPass();

        verify(newsImportService, never()).importRssFeeds();
    }

    @Test
    @DisplayName("An enabled source runs its pass")
    void passes_runWhenEnabled() {
        when(newsImportSettingsService.isScheduledPassEnabled(NewsSource.STEAM)).thenReturn(true);
        when(newsImportSettingsService.isScheduledPassEnabled(NewsSource.RSS)).thenReturn(true);

        task.runSteamPass();
        task.runRssPass();

        verify(newsImportService).importSteamNews();
        verify(newsImportService).importRssFeeds();
    }

    @Test
    @DisplayName("A failing pass is swallowed so the scheduler keeps its rhythm")
    void failingPass_doesNotEscape() {
        when(newsImportSettingsService.isScheduledPassEnabled(NewsSource.STEAM)).thenReturn(true);
        when(newsImportService.importSteamNews()).thenThrow(new RuntimeException("Steam 503"));

        task.runSteamPass();

        verify(newsImportService).importSteamNews();
    }
}
