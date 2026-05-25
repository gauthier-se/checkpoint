package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;

@ExtendWith(MockitoExtension.class)
class GameSimilarityServiceImplTest {

    private static final UUID ANONYMOUS_VIEWER = new UUID(0L, 0L);

    @Mock private VideoGameRepository videoGameRepository;
    @Mock private UserRepository userRepository;

    private GameSimilarityServiceImpl service;

    private Genre rpgGenre;
    private Genre actionGenre;
    private Company studioA;
    private Company studioB;
    private Platform pcPlatform;

    @BeforeEach
    void setUp() {
        service = new GameSimilarityServiceImpl(videoGameRepository, userRepository);

        rpgGenre = newGenre("RPG");
        actionGenre = newGenre("Action");
        studioA = newCompany("Studio A");
        studioB = newCompany("Studio B");
        pcPlatform = newPlatform("PC");
    }

    @Test
    @DisplayName("unknown seed game throws GameNotFoundException")
    void unknownGameThrows() {
        UUID missingId = UUID.randomUUID();
        when(videoGameRepository.findByIdWithRelationships(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSimilarGames(missingId, null, 12))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    @DisplayName("seed without genres or companies returns empty and skips the candidate query")
    void seedWithoutTagsReturnsEmpty() {
        VideoGame seed = newGame("Tagless", List.of(), List.of(pcPlatform), List.of());
        when(videoGameRepository.findByIdWithRelationships(seed.getId())).thenReturn(Optional.of(seed));

        List<GameCardDto> result = service.getSimilarGames(seed.getId(), null, 12);

        assertThat(result).isEmpty();
        verify(videoGameRepository, never())
                .findSimilarCandidateIds(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("candidates are ranked by shared tag overlap and the ranked order is preserved")
    void ranksBySharedTagOverlap() {
        VideoGame seed = newGame("Seed RPG", List.of(rpgGenre, actionGenre), List.of(pcPlatform), List.of(studioA));
        when(videoGameRepository.findByIdWithRelationships(seed.getId())).thenReturn(Optional.of(seed));

        // Strong shares two genres + platform + studio; weak shares only one genre.
        VideoGame strong = newGame("Strong Match", List.of(rpgGenre, actionGenre), List.of(pcPlatform), List.of(studioA));
        VideoGame weak = newGame("Weak Match", List.of(rpgGenre), List.of(), List.of(studioB));

        List<UUID> candidateIds = List.of(strong.getId(), weak.getId());
        when(videoGameRepository.findSimilarCandidateIds(
                eq(seed.getId()), any(), any(), any(), any(Pageable.class)))
                .thenReturn(candidateIds);
        when(videoGameRepository.findAllByIdInWithRelationships(candidateIds))
                .thenReturn(List.of(strong, weak));

        // Returned in arbitrary order — the service must restore the ranked order.
        when(videoGameRepository.findGameCardsByIdIn(List.of(strong.getId(), weak.getId())))
                .thenReturn(List.of(card(weak), card(strong)));

        List<GameCardDto> result = service.getSimilarGames(seed.getId(), null, 12);

        assertThat(result).extracting(GameCardDto::id)
                .containsExactly(strong.getId(), weak.getId());
    }

    @Test
    @DisplayName("empty candidate pool returns empty without hydrating entities")
    void emptyCandidatePoolReturnsEmpty() {
        VideoGame seed = newGame("Seed", List.of(rpgGenre), List.of(), List.of(studioA));
        when(videoGameRepository.findByIdWithRelationships(seed.getId())).thenReturn(Optional.of(seed));
        when(videoGameRepository.findSimilarCandidateIds(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        List<GameCardDto> result = service.getSimilarGames(seed.getId(), null, 12);

        assertThat(result).isEmpty();
        verify(videoGameRepository, never()).findAllByIdInWithRelationships(any());
    }

    @Test
    @DisplayName("anonymous viewer uses the sentinel id and never looks up a user")
    void anonymousViewerUsesSentinel() {
        VideoGame seed = newGame("Seed", List.of(rpgGenre), List.of(), List.of(studioA));
        when(videoGameRepository.findByIdWithRelationships(seed.getId())).thenReturn(Optional.of(seed));
        when(videoGameRepository.findSimilarCandidateIds(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        service.getSimilarGames(seed.getId(), null, 12);

        verify(videoGameRepository).findSimilarCandidateIds(
                eq(seed.getId()), eq(ANONYMOUS_VIEWER), any(), any(), any(Pageable.class));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("authenticated viewer's id is resolved and passed to the exclusion query")
    void authenticatedViewerIdResolved() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("player@example.com");
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));

        VideoGame seed = newGame("Seed", List.of(rpgGenre), List.of(), List.of(studioA));
        when(videoGameRepository.findByIdWithRelationships(seed.getId())).thenReturn(Optional.of(seed));
        when(videoGameRepository.findSimilarCandidateIds(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        service.getSimilarGames(seed.getId(), "player@example.com", 12);

        verify(videoGameRepository).findSimilarCandidateIds(
                eq(seed.getId()), eq(userId), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("results are limited to the requested size")
    void limitsResultsToRequestedSize() {
        VideoGame seed = newGame("Seed", List.of(rpgGenre), List.of(), List.of(studioA));
        when(videoGameRepository.findByIdWithRelationships(seed.getId())).thenReturn(Optional.of(seed));

        List<UUID> candidateIds = new ArrayList<>();
        List<VideoGame> candidateGames = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            VideoGame c = newGame("Cand " + i, List.of(rpgGenre), List.of(), List.of(studioA));
            candidateIds.add(c.getId());
            candidateGames.add(c);
        }
        when(videoGameRepository.findSimilarCandidateIds(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(candidateIds);
        when(videoGameRepository.findAllByIdInWithRelationships(candidateIds)).thenReturn(candidateGames);
        when(videoGameRepository.findGameCardsByIdIn(any())).thenAnswer(invocation -> {
            List<UUID> ids = invocation.getArgument(0);
            return ids.stream().map(id ->
                    new GameCardDto(id, "t", "cover.jpg", LocalDate.now().minusYears(3), null, 0L)).toList();
        });

        List<GameCardDto> result = service.getSimilarGames(seed.getId(), null, 2);

        assertThat(result).hasSize(2);
    }

    // ----- helpers -----

    private static GameCardDto card(VideoGame game) {
        return new GameCardDto(game.getId(), game.getTitle(), "cover.jpg", game.getReleaseDate(), null, 0L);
    }

    private static Genre newGenre(String name) {
        Genre g = new Genre(name);
        g.setId(UUID.randomUUID());
        return g;
    }

    private static Company newCompany(String name) {
        Company c = new Company(name);
        c.setId(UUID.randomUUID());
        return c;
    }

    private static Platform newPlatform(String name) {
        Platform p = new Platform();
        p.setId(UUID.randomUUID());
        p.setName(name);
        return p;
    }

    private static VideoGame newGame(String title,
                                     Collection<Genre> genres,
                                     Collection<Platform> platforms,
                                     Collection<Company> companies) {
        VideoGame g = new VideoGame();
        g.setId(UUID.randomUUID());
        g.setTitle(title);
        g.setReleaseDate(LocalDate.now().minusYears(3));
        g.setGenres(new java.util.HashSet<>(genres));
        g.setPlatforms(new java.util.HashSet<>(platforms));
        g.setCompanies(new java.util.HashSet<>(companies));
        return g;
    }
}
