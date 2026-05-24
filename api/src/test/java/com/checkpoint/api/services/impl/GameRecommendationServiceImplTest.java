package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.RecommendedGameDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.GameStatus;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.GameTrendingService;

@ExtendWith(MockitoExtension.class)
class GameRecommendationServiceImplTest {

    private static final String USER_EMAIL = "player@example.com";

    @Mock private UserRepository userRepository;
    @Mock private RateRepository rateRepository;
    @Mock private UserGameRepository userGameRepository;
    @Mock private WishRepository wishRepository;
    @Mock private VideoGameRepository videoGameRepository;
    @Mock private GameTrendingService gameTrendingService;

    private GameRecommendationServiceImpl service;

    private User user;
    private UUID userId;

    private Genre rpgGenre;
    private Genre actionGenre;
    private Genre puzzleGenre;
    private Company studioA;
    private Company studioB;
    private Platform pcPlatform;

    @BeforeEach
    void setUp() {
        service = new GameRecommendationServiceImpl(
                userRepository, rateRepository, userGameRepository,
                wishRepository, videoGameRepository, gameTrendingService);

        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail(USER_EMAIL);

        rpgGenre = newGenre("RPG");
        actionGenre = newGenre("Action");
        puzzleGenre = newGenre("Puzzle");
        studioA = newCompany("Studio A");
        studioB = newCompany("Studio B");
        pcPlatform = newPlatform("PC");

        lenient().when(userRepository.findByEmail(USER_EMAIL)).thenReturn(java.util.Optional.of(user));
    }

    @Nested
    @DisplayName("cold-start")
    class ColdStart {

        @Test
        @DisplayName("user with no rates / library / wishes falls back to trending games")
        void coldStartFallsBackToTrending() {
            when(rateRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(userGameRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(wishRepository.findVideoGameIdsByUserId(userId)).thenReturn(List.of());

            UUID trendingId = UUID.randomUUID();
            GameCardDto trending = new GameCardDto(
                    trendingId, "Trending Game", "cover.jpg",
                    LocalDate.now().minusMonths(1), 4.2, 100L);
            when(gameTrendingService.getTrendingGames(10)).thenReturn(List.of(trending));

            List<RecommendedGameDto> result = service.getRecommendationsFor(USER_EMAIL, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(trendingId);
            assertThat(result.get(0).reason()).isEqualTo("Trending this week");
            verifyNoInteractions(videoGameRepository);
        }

        @Test
        @DisplayName("user with only low-scoring rates (<6) still cold-starts")
        void lowScoresContributeNothing() {
            VideoGame disliked = newGame("Disliked", List.of(rpgGenre), List.of(), List.of(studioA));
            Rate lowRate = new Rate(user, disliked, 3);
            when(rateRepository.findAllByUserId(userId)).thenReturn(List.of(lowRate));
            when(userGameRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(wishRepository.findVideoGameIdsByUserId(userId)).thenReturn(List.of());

            when(gameTrendingService.getTrendingGames(anyInt())).thenReturn(List.of());

            List<RecommendedGameDto> result = service.getRecommendationsFor(USER_EMAIL, 10);

            assertThat(result).isEmpty();
            verify(gameTrendingService, times(1)).getTrendingGames(10);
        }
    }

    @Nested
    @DisplayName("scoring")
    class Scoring {

        @Test
        @DisplayName("RPGs surface for a user who completed RPGs")
        void completedRpgsSurfaceRpgCandidates() {
            VideoGame likedRpg = newGame("Liked RPG", List.of(rpgGenre), List.of(pcPlatform), List.of(studioA));
            UserGame ug = new UserGame(user, likedRpg, GameStatus.COMPLETED);
            when(rateRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(userGameRepository.findAllByUserId(userId)).thenReturn(List.of(ug));
            when(wishRepository.findVideoGameIdsByUserId(userId)).thenReturn(List.of());

            when(videoGameRepository.findAllByIdInWithRelationships(Set.of(likedRpg.getId())))
                    .thenReturn(List.of(likedRpg));

            VideoGame candidateRpg = newGame("Other RPG", List.of(rpgGenre), List.of(pcPlatform), List.of(studioB));
            VideoGame candidatePuzzle = newGame("Puzzle Game", List.of(puzzleGenre), List.of(pcPlatform), List.of(studioB));
            candidateRpg.setAverageRating(4.0);
            candidatePuzzle.setAverageRating(4.0);

            // Only the RPG candidate matches the prefilter (shares a genre).
            when(videoGameRepository.findCandidateIdsForRecommendation(
                    eq(userId), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(candidateRpg.getId()));
            when(videoGameRepository.findAllByIdInWithRelationships(List.of(candidateRpg.getId())))
                    .thenReturn(List.of(candidateRpg));

            List<RecommendedGameDto> result = service.getRecommendationsFor(USER_EMAIL, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(candidateRpg.getId());
            assertThat(result.get(0).reason()).contains("RPG");
        }

        @Test
        @DisplayName("higher rated library games dominate the affinity profile")
        void highRatedGamesDominateProfile() {
            VideoGame ratedHigh = newGame("Rated High", List.of(rpgGenre), List.of(), List.of(studioA));
            VideoGame ratedMid = newGame("Rated Mid", List.of(actionGenre), List.of(), List.of(studioB));
            Rate high = new Rate(user, ratedHigh, 9);
            Rate mid = new Rate(user, ratedMid, 6);
            when(rateRepository.findAllByUserId(userId)).thenReturn(List.of(high, mid));
            when(userGameRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(wishRepository.findVideoGameIdsByUserId(userId)).thenReturn(List.of());

            when(videoGameRepository.findAllByIdInWithRelationships(any()))
                    .thenReturn(List.of(ratedHigh, ratedMid));

            VideoGame rpgCandidate = newGame("New RPG", List.of(rpgGenre), List.of(), List.of(studioB));
            VideoGame actionCandidate = newGame("New Action", List.of(actionGenre), List.of(), List.of(studioB));

            when(videoGameRepository.findCandidateIdsForRecommendation(
                    eq(userId), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(rpgCandidate.getId(), actionCandidate.getId()));
            when(videoGameRepository.findAllByIdInWithRelationships(List.of(rpgCandidate.getId(), actionCandidate.getId())))
                    .thenReturn(List.of(rpgCandidate, actionCandidate));

            List<RecommendedGameDto> result = service.getRecommendationsFor(USER_EMAIL, 5);

            // Rated 9 → weight 2.0 on RPG ; Rated 6 → weight 1.0 on Action.
            // So RPG candidate must rank above Action candidate.
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(rpgCandidate.getId());
            assertThat(result.get(1).id()).isEqualTo(actionCandidate.getId());
        }

        @Test
        @DisplayName("empty candidate pool falls back to trending")
        void emptyCandidatePoolFallsBackToTrending() {
            VideoGame liked = newGame("Liked", List.of(rpgGenre), List.of(), List.of(studioA));
            UserGame ug = new UserGame(user, liked, GameStatus.COMPLETED);
            when(rateRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(userGameRepository.findAllByUserId(userId)).thenReturn(List.of(ug));
            when(wishRepository.findVideoGameIdsByUserId(userId)).thenReturn(List.of());

            when(videoGameRepository.findAllByIdInWithRelationships(Set.of(liked.getId())))
                    .thenReturn(List.of(liked));
            when(videoGameRepository.findCandidateIdsForRecommendation(
                    eq(userId), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of());

            UUID trendingId = UUID.randomUUID();
            when(gameTrendingService.getTrendingGames(5)).thenReturn(List.of(
                    new GameCardDto(trendingId, "Trending", "c.jpg",
                            LocalDate.now(), 4.5, 50L)));

            List<RecommendedGameDto> result = service.getRecommendationsFor(USER_EMAIL, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(trendingId);
            assertThat(result.get(0).reason()).isEqualTo("Trending this week");
        }

        @Test
        @DisplayName("wishlist-only profile still produces a tag-overlap recommendation")
        void wishOnlyProfileProducesRecommendation() {
            VideoGame wished = newGame("Wished", List.of(actionGenre), List.of(), List.of(studioA));
            when(rateRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(userGameRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(wishRepository.findVideoGameIdsByUserId(userId)).thenReturn(List.of(wished.getId()));

            when(videoGameRepository.findAllByIdInWithRelationships(Set.of(wished.getId())))
                    .thenReturn(List.of(wished));

            VideoGame candidate = newGame("Action Sequel", List.of(actionGenre), List.of(), List.of(studioA));
            when(videoGameRepository.findCandidateIdsForRecommendation(
                    eq(userId), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(candidate.getId()));
            when(videoGameRepository.findAllByIdInWithRelationships(List.of(candidate.getId())))
                    .thenReturn(List.of(candidate));

            List<RecommendedGameDto> result = service.getRecommendationsFor(USER_EMAIL, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(candidate.getId());
            assertThat(result.get(0).reason()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("size validation")
    class SizeValidation {

        @Test
        @DisplayName("size <= 0 falls back to default 10")
        void nonPositiveSizeDefaultsTo10() {
            when(rateRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(userGameRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(wishRepository.findVideoGameIdsByUserId(userId)).thenReturn(List.of());
            when(gameTrendingService.getTrendingGames(10)).thenReturn(Collections.emptyList());

            service.getRecommendationsFor(USER_EMAIL, 0);
            verify(gameTrendingService).getTrendingGames(10);
        }

        @Test
        @DisplayName("size > 30 is capped at 30")
        void sizeCappedAt30() {
            when(rateRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(userGameRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(wishRepository.findVideoGameIdsByUserId(userId)).thenReturn(List.of());
            when(gameTrendingService.getTrendingGames(30)).thenReturn(Collections.emptyList());

            service.getRecommendationsFor(USER_EMAIL, 999);
            verify(gameTrendingService).getTrendingGames(30);
        }
    }

    // ----- helpers -----

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
        // ensure mutable copies
        g.setGenres(new java.util.HashSet<>(new ArrayList<>(genres)));
        g.setPlatforms(new java.util.HashSet<>(new ArrayList<>(platforms)));
        g.setCompanies(new java.util.HashSet<>(new ArrayList<>(companies)));
        return g;
    }
}
