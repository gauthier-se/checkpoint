package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.dto.catalog.NewsImportSettingsDto;
import com.checkpoint.api.dto.catalog.NewsImportSettingsRequestDto;
import com.checkpoint.api.entities.NewsImportSettings;
import com.checkpoint.api.entities.NewsSource;
import com.checkpoint.api.repositories.NewsImportSettingsRepository;
import com.checkpoint.api.repositories.NewsRepository;

/**
 * Unit tests for {@link NewsImportSettingsServiceImpl}: default-on-empty reads,
 * partial updates, bounds checking and the derived daily budget.
 */
@ExtendWith(MockitoExtension.class)
class NewsImportSettingsServiceImplTest {

    @Mock private NewsImportSettingsRepository settingsRepository;
    @Mock private NewsRepository newsRepository;

    private NewsImportSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NewsImportSettingsServiceImpl(settingsRepository, newsRepository);
        lenient().when(newsRepository.countBySourceInAndCreatedAtGreaterThanEqual(anyCollection(), any()))
                .thenReturn(0L);
        lenient().when(settingsRepository.save(any(NewsImportSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private NewsImportSettings storedSettings() {
        NewsImportSettings settings = new NewsImportSettings();
        when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        return settings;
    }

    private void noStoredSettings() {
        when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("An untouched installation reads as the defaults, without inserting a row")
    void get_defaultsWithoutWriting() {
        noStoredSettings();

        NewsImportSettingsDto dto = service.get();

        assertThat(dto.steamEnabled()).isTrue();
        assertThat(dto.rssEnabled()).isTrue();
        assertThat(dto.maxArticlesPerDay())
                .isEqualTo(NewsImportSettings.DEFAULT_MAX_ARTICLES_PER_DAY);
        assertThat(dto.steamNewsPerGame())
                .isEqualTo(NewsImportSettings.DEFAULT_STEAM_NEWS_PER_GAME);
        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Today's counters are derived from the news table, not stored")
    void get_reportsTodayCounters() {
        storedSettings().setMaxArticlesPerDay(50);
        when(newsRepository.countBySourceInAndCreatedAtGreaterThanEqual(anyCollection(), any()))
                .thenReturn(12L);

        NewsImportSettingsDto dto = service.get();

        assertThat(dto.importedToday()).isEqualTo(12L);
        assertThat(dto.remainingToday()).isEqualTo(38);
    }

    @Test
    @DisplayName("An uncapped setup reports no remaining count and an unlimited budget")
    void uncapped_hasNoRemaining() {
        storedSettings().setMaxArticlesPerDay(null);

        assertThat(service.get().remainingToday()).isNull();
        assertThat(service.remainingDailyBudget()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("The budget floors at zero when the day already overshot the ceiling")
    void budget_neverGoesNegative() {
        storedSettings().setMaxArticlesPerDay(10);
        when(newsRepository.countBySourceInAndCreatedAtGreaterThanEqual(anyCollection(), any()))
                .thenReturn(25L);

        assertThat(service.remainingDailyBudget()).isZero();
        assertThat(service.get().remainingToday()).isZero();
    }

    @Test
    @DisplayName("A partial update leaves the fields it does not mention alone")
    void update_isPartial() {
        NewsImportSettings stored = storedSettings();
        stored.setMaxArticlesPerDay(50);

        NewsImportSettingsDto dto = service.update("admin",
                new NewsImportSettingsRequestDto(false, null, null, null, null));

        assertThat(dto.steamEnabled()).isFalse();
        assertThat(dto.rssEnabled()).isTrue();
        assertThat(dto.maxArticlesPerDay()).isEqualTo(50);
        assertThat(dto.updatedBy()).isEqualTo("admin");
        verify(settingsRepository).save(stored);
    }

    @Test
    @DisplayName("The unlimited flag is the only way to clear the ceiling")
    void update_unlimitedClearsCeiling() {
        storedSettings().setMaxArticlesPerDay(50);

        NewsImportSettingsDto dto = service.update("admin",
                new NewsImportSettingsRequestDto(null, null, 999, null, true));

        assertThat(dto.maxArticlesPerDay()).isNull();
        assertThat(dto.remainingToday()).isNull();
    }

    @Test
    @DisplayName("The first save creates the row from the in-memory defaults")
    void update_createsTheRowOnFirstSave() {
        noStoredSettings();

        service.update("admin", new NewsImportSettingsRequestDto(null, false, null, null, null));

        verify(settingsRepository).save(any(NewsImportSettings.class));
    }

    @Test
    @DisplayName("Out-of-range numbers are rejected as bad requests")
    void update_rejectsOutOfRangeNumbers() {
        lenient().when(settingsRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(new NewsImportSettings()));

        assertThatThrownBy(() -> service.update("admin",
                new NewsImportSettingsRequestDto(null, null, null, 0, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("steamNewsPerGame");

        assertThatThrownBy(() -> service.update("admin",
                new NewsImportSettingsRequestDto(null, null, -1, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxArticlesPerDay");
    }

    @Test
    @DisplayName("Only the matching source toggle gates a scheduled pass, and MANUAL has none")
    void isScheduledPassEnabled_perSource() {
        NewsImportSettings stored = storedSettings();
        stored.setSteamEnabled(false);

        assertThat(service.isScheduledPassEnabled(NewsSource.STEAM)).isFalse();
        assertThat(service.isScheduledPassEnabled(NewsSource.RSS)).isTrue();
        assertThat(service.isScheduledPassEnabled(NewsSource.MANUAL)).isFalse();
    }
}
