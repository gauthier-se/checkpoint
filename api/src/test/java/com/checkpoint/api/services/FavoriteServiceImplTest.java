package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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

import com.checkpoint.api.dto.profile.FavoriteDto;
import com.checkpoint.api.entities.Favorite;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.InvalidFavoritesException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.mapper.ProfileMapper;
import com.checkpoint.api.repositories.FavoriteRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.impl.FavoriteServiceImpl;

/**
 * Unit tests for {@link FavoriteServiceImpl#replaceFavorites}.
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoGameRepository videoGameRepository;

    @Mock
    private ProfileMapper profileMapper;

    private FavoriteServiceImpl service;

    private User testUser;

    @BeforeEach
    void setUp() {
        service = new FavoriteServiceImpl(
                favoriteRepository, userRepository, videoGameRepository, profileMapper);
        testUser = new User("alice", "alice@test.com", "encoded-pwd");
        testUser.setId(UUID.randomUUID());
    }

    private VideoGame newGame(UUID id, String title) {
        VideoGame g = new VideoGame(title, "desc", LocalDate.now());
        g.setId(id);
        return g;
    }

    @Test
    @DisplayName("Happy path: replace persists favorites with 0-based displayOrder")
    void replaceFavorites_persistsZeroBasedOrder() {
        UUID g1 = UUID.randomUUID();
        UUID g2 = UUID.randomUUID();
        UUID g3 = UUID.randomUUID();
        List<UUID> ids = List.of(g1, g2, g3);

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));
        when(videoGameRepository.findAllById(ids))
                .thenReturn(List.of(newGame(g1, "G1"), newGame(g2, "G2"), newGame(g3, "G3")));
        when(favoriteRepository.findByUserOrderByDisplayOrderAsc(testUser))
                .thenReturn(List.of());

        service.replaceFavorites("alice@test.com", ids);

        verify(favoriteRepository).deleteByUser(testUser);
        verify(favoriteRepository).flush();

        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository, times(3)).save(captor.capture());

        List<Favorite> saved = captor.getAllValues();
        assertThat(saved).hasSize(3);
        assertThat(saved.get(0).getDisplayOrder()).isEqualTo(0);
        assertThat(saved.get(1).getDisplayOrder()).isEqualTo(1);
        assertThat(saved.get(2).getDisplayOrder()).isEqualTo(2);
        assertThat(saved.get(0).getVideoGame().getId()).isEqualTo(g1);
        assertThat(saved.get(1).getVideoGame().getId()).isEqualTo(g2);
        assertThat(saved.get(2).getVideoGame().getId()).isEqualTo(g3);
    }

    @Test
    @DisplayName("Empty list clears favorites and saves nothing")
    void replaceFavorites_emptyClearsAndSavesNothing() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));
        when(favoriteRepository.findByUserOrderByDisplayOrderAsc(testUser)).thenReturn(List.of());

        List<FavoriteDto> result = service.replaceFavorites("alice@test.com", List.of());

        assertThat(result).isEmpty();
        verify(favoriteRepository).deleteByUser(testUser);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Rejects more than 5 ids with InvalidFavoritesException")
    void replaceFavorites_rejectsOverLimit() {
        List<UUID> ids = List.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> service.replaceFavorites("alice@test.com", ids))
                .isInstanceOf(InvalidFavoritesException.class)
                .hasMessageContaining("at most 5");

        verify(userRepository, never()).findByEmail(any());
        verify(favoriteRepository, never()).deleteByUser(any());
    }

    @Test
    @DisplayName("Rejects duplicate gameIds")
    void replaceFavorites_rejectsDuplicates() {
        UUID g = UUID.randomUUID();
        List<UUID> ids = List.of(g, g);

        assertThatThrownBy(() -> service.replaceFavorites("alice@test.com", ids))
                .isInstanceOf(InvalidFavoritesException.class)
                .hasMessageContaining("Duplicate");

        verify(userRepository, never()).findByEmail(any());
        verify(favoriteRepository, never()).deleteByUser(any());
    }

    @Test
    @DisplayName("Rejects unknown gameId (repository returns fewer games than requested)")
    void replaceFavorites_rejectsUnknownGameId() {
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        List<UUID> ids = List.of(known, unknown);

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));
        when(videoGameRepository.findAllById(ids)).thenReturn(List.of(newGame(known, "G")));

        assertThatThrownBy(() -> service.replaceFavorites("alice@test.com", ids))
                .isInstanceOf(InvalidFavoritesException.class)
                .hasMessageContaining("do not exist");

        verify(favoriteRepository, never()).deleteByUser(any());
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Throws UserNotFoundException when the email has no matching user")
    void replaceFavorites_throwsWhenUserMissing() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replaceFavorites("ghost@test.com", List.of()))
                .isInstanceOf(UserNotFoundException.class);

        verify(favoriteRepository, never()).deleteByUser(any());
    }

    @Test
    @DisplayName("getFavorites delegates to repository ordered by displayOrder")
    void getFavorites_delegatesToRepository() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(favoriteRepository.findByUserOrderByDisplayOrderAsc(testUser)).thenReturn(List.of());

        List<FavoriteDto> result = service.getFavorites(testUser.getId());

        assertThat(result).isEmpty();
        verify(favoriteRepository).findByUserOrderByDisplayOrderAsc(testUser);
    }

    // Silence "unused" warning if we ever change shape; argMatcher convenience.
    @SuppressWarnings("unused")
    private static List<UUID> anyIdList() {
        return anyList();
    }
}
