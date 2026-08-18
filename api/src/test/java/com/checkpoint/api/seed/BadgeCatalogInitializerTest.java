package com.checkpoint.api.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.checkpoint.api.entities.Badge;
import com.checkpoint.api.enums.BadgeCode;
import com.checkpoint.api.repositories.BadgeRepository;

/**
 * Unit tests for {@link BadgeCatalogInitializer}.
 */
@ExtendWith(MockitoExtension.class)
class BadgeCatalogInitializerTest {

    @Mock
    private BadgeRepository badgeRepository;

    private BadgeCatalogInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new BadgeCatalogInitializer(badgeRepository);
    }

    @Test
    @DisplayName("inserts every catalog code on an empty database")
    void insertsWholeCatalogWhenEmpty() {
        when(badgeRepository.findByCode(anyString())).thenReturn(Optional.empty());

        initializer.run(null);

        ArgumentCaptor<Badge> saved = ArgumentCaptor.forClass(Badge.class);
        verify(badgeRepository, org.mockito.Mockito.times(BadgeCode.values().length)).save(saved.capture());

        List<String> codes = new ArrayList<>(saved.getAllValues().stream().map(Badge::getCode).toList());
        assertThat(codes).containsExactlyInAnyOrderElementsOf(
                List.of(BadgeCode.values()).stream().map(Enum::name).toList());
    }

    @Test
    @DisplayName("leaves rows already in sync untouched, preserving DB-side name edits")
    void keepsExistingRowsUntouched() {
        for (BadgeCode code : BadgeCode.values()) {
            Badge existing = new Badge(code.name(), "Admin renamed", "Admin description", null, code.isHidden());
            when(badgeRepository.findByCode(code.name())).thenReturn(Optional.of(existing));
        }

        initializer.run(null);

        verify(badgeRepository, never()).save(any(Badge.class));
    }

    @Test
    @DisplayName("re-syncs the hidden flag when it drifts from the enum")
    void resyncsHiddenFlag() {
        Badge drifted = null;
        for (BadgeCode code : BadgeCode.values()) {
            Badge existing = new Badge(code.name(), code.getDefaultName(), code.getDefaultDescription(),
                    null, code.isHidden());
            if (drifted == null && code.isHidden()) {
                existing.setHidden(false);
                drifted = existing;
            }
            when(badgeRepository.findByCode(code.name())).thenReturn(Optional.of(existing));
        }

        initializer.run(null);

        assertThat(drifted).as("catalog must keep at least one hidden badge").isNotNull();
        assertThat(drifted.isHidden()).isTrue();
        verify(badgeRepository).save(drifted);
    }
}
