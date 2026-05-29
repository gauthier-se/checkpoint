package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.profile.CommonGameEntryDto;
import com.checkpoint.api.dto.profile.ProfileComparisonDto;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.exceptions.ProfilePrivateException;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;

/**
 * Unit tests for {@link ProfileComparisonServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ProfileComparisonServiceImplTest {

    private static final String VIEWER_EMAIL = "viewer@example.com";
    private static final String TARGET_USERNAME = "target";
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private RateRepository rateRepository;

    private ProfileComparisonServiceImpl service;

    private User viewer;
    private User target;

    @BeforeEach
    void setUp() {
        service = new ProfileComparisonServiceImpl(userRepository, userGameRepository, rateRepository);

        viewer = new User();
        viewer.setId(UUID.randomUUID());
        viewer.setEmail(VIEWER_EMAIL);
        viewer.setPseudo("viewer");
        viewer.setIsPrivate(false);

        target = new User();
        target.setId(UUID.randomUUID());
        target.setEmail("target@example.com");
        target.setPseudo(TARGET_USERNAME);
        target.setIsPrivate(false);
    }

    @Test
    @DisplayName("should compute affinity score from library overlap and rating similarity")
    void compare_knownInputs_computesAffinityScore() {
        // Given: 3 common games out of a 5-game union -> libraryScore = 60
        VideoGame g1 = game("Elden Ring");
        VideoGame g2 = game("Hades");
        VideoGame g3 = game("Celeste");
        List<UUID> commonIds = List.of(g1.getId(), g2.getId(), g3.getId());

        stubUsersFound();
        when(userGameRepository.findCommonVideoGameIds(viewer.getId(), target.getId()))
                .thenReturn(commonIds);
        when(userGameRepository.countDistinctGamesByUserIds(viewer.getId(), target.getId()))
                .thenReturn(5L);
        when(userGameRepository.countByUserId(viewer.getId())).thenReturn(4L);
        when(userGameRepository.countByUserId(target.getId())).thenReturn(4L);

        when(userGameRepository.findByUserIdAndVideoGameIdIn(viewer.getId(), commonIds))
                .thenReturn(List.of(
                        userGame(viewer, g1, PlayStatus.COMPLETED),
                        userGame(viewer, g2, PlayStatus.ARE_PLAYING),
                        userGame(viewer, g3, PlayStatus.ARE_PLAYING)));
        when(userGameRepository.findByUserIdAndVideoGameIdIn(target.getId(), commonIds))
                .thenReturn(List.of(
                        userGame(target, g1, PlayStatus.COMPLETED),
                        userGame(target, g2, PlayStatus.COMPLETED),
                        userGame(target, g3, PlayStatus.ABANDONED)));

        // Ratings (raw 1-10): g1 viewer 10/target 8 -> 5.0 vs 4.0 (diff 1.0)
        //                     g2 viewer 6/target 6  -> 3.0 vs 3.0 (diff 0.0)
        //                     g3 viewer 4/target -   -> 2.0 vs null (diff null)
        when(rateRepository.findByUserIdInAndVideoGameIdIn(
                List.of(viewer.getId(), target.getId()), commonIds))
                .thenReturn(List.of(
                        rate(viewer, g1, 10),
                        rate(target, g1, 8),
                        rate(viewer, g2, 6),
                        rate(target, g2, 6),
                        rate(viewer, g3, 4)));

        // When
        ProfileComparisonDto result = service.compare(VIEWER_EMAIL, TARGET_USERNAME, PAGEABLE);

        // Then: avgDiff = (1.0 + 0.0) / 2 = 0.5 -> ratingScore = (1 - 0.5/4)*100 = 87.5
        //       final = round(0.6*60 + 0.4*87.5) = round(36 + 35) = 71
        assertThat(result.affinityScore()).isEqualTo(71);
        assertThat(result.commonGamesCount()).isEqualTo(3);
        assertThat(result.viewerLibrarySize()).isEqualTo(4);
        assertThat(result.targetLibrarySize()).isEqualTo(4);

        // Sorted by ratingDiff DESC NULLS LAST: g1 (1.0), g2 (0.0), g3 (null)
        List<CommonGameEntryDto> content = result.commonGames().content();
        assertThat(content).hasSize(3);
        assertThat(content.get(0).title()).isEqualTo("Elden Ring");
        assertThat(content.get(0).viewerRating()).isEqualTo(5.0);
        assertThat(content.get(0).targetRating()).isEqualTo(4.0);
        assertThat(content.get(0).ratingDiff()).isEqualTo(1.0);
        assertThat(content.get(0).viewerStatus()).isEqualTo(PlayStatus.COMPLETED);
        assertThat(content.get(0).targetStatus()).isEqualTo(PlayStatus.COMPLETED);
        assertThat(content.get(1).title()).isEqualTo("Hades");
        assertThat(content.get(1).ratingDiff()).isEqualTo(0.0);
        assertThat(content.get(2).title()).isEqualTo("Celeste");
        assertThat(content.get(2).viewerRating()).isEqualTo(2.0);
        assertThat(content.get(2).targetRating()).isNull();
        assertThat(content.get(2).ratingDiff()).isNull();
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when comparing with own profile")
    void compare_withSelf_throwsIllegalArgument() {
        when(userRepository.findByEmail(VIEWER_EMAIL)).thenReturn(Optional.of(viewer));
        when(userRepository.findByPseudo("viewer")).thenReturn(Optional.of(viewer));

        assertThatThrownBy(() -> service.compare(VIEWER_EMAIL, "viewer", PAGEABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itself");
    }

    @Test
    @DisplayName("should throw ProfilePrivateException when target is private and viewer does not follow")
    void compare_privateTargetNotFollowing_throwsProfilePrivate() {
        target.setIsPrivate(true);
        stubUsersFound();
        when(userRepository.isFollowing(viewer.getId(), target.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.compare(VIEWER_EMAIL, TARGET_USERNAME, PAGEABLE))
                .isInstanceOf(ProfilePrivateException.class);
    }

    @Test
    @DisplayName("should allow comparison when target is private but viewer follows them")
    void compare_privateTargetFollowing_succeeds() {
        target.setIsPrivate(true);
        stubUsersFound();
        when(userRepository.isFollowing(viewer.getId(), target.getId())).thenReturn(true);
        when(userGameRepository.findCommonVideoGameIds(viewer.getId(), target.getId()))
                .thenReturn(List.of());
        when(userGameRepository.countDistinctGamesByUserIds(viewer.getId(), target.getId()))
                .thenReturn(2L);
        when(userGameRepository.countByUserId(viewer.getId())).thenReturn(1L);
        when(userGameRepository.countByUserId(target.getId())).thenReturn(1L);

        ProfileComparisonDto result = service.compare(VIEWER_EMAIL, TARGET_USERNAME, PAGEABLE);

        assertThat(result.affinityScore()).isZero();
        assertThat(result.commonGamesCount()).isZero();
    }

    @Test
    @DisplayName("should return score 0 when users share no games")
    void compare_noCommonGames_scoreIsZero() {
        stubUsersFound();
        when(userGameRepository.findCommonVideoGameIds(viewer.getId(), target.getId()))
                .thenReturn(List.of());
        when(userGameRepository.countDistinctGamesByUserIds(viewer.getId(), target.getId()))
                .thenReturn(6L);
        when(userGameRepository.countByUserId(viewer.getId())).thenReturn(3L);
        when(userGameRepository.countByUserId(target.getId())).thenReturn(3L);

        ProfileComparisonDto result = service.compare(VIEWER_EMAIL, TARGET_USERNAME, PAGEABLE);

        assertThat(result.affinityScore()).isZero();
        assertThat(result.commonGamesCount()).isZero();
        assertThat(result.commonGames().content()).isEmpty();
    }

    @Test
    @DisplayName("should omit the rating component (score = library score) when no common game is rated by both")
    void compare_commonGamesNoCommonRatings_scoreEqualsLibraryScore() {
        VideoGame g1 = game("Hollow Knight");
        List<UUID> commonIds = List.of(g1.getId());

        stubUsersFound();
        when(userGameRepository.findCommonVideoGameIds(viewer.getId(), target.getId()))
                .thenReturn(commonIds);
        when(userGameRepository.countDistinctGamesByUserIds(viewer.getId(), target.getId()))
                .thenReturn(3L);
        when(userGameRepository.countByUserId(viewer.getId())).thenReturn(2L);
        when(userGameRepository.countByUserId(target.getId())).thenReturn(2L);
        when(userGameRepository.findByUserIdAndVideoGameIdIn(viewer.getId(), commonIds))
                .thenReturn(List.of(userGame(viewer, g1, PlayStatus.COMPLETED)));
        when(userGameRepository.findByUserIdAndVideoGameIdIn(target.getId(), commonIds))
                .thenReturn(List.of(userGame(target, g1, PlayStatus.ARE_PLAYING)));
        // Only the viewer rated the game -> no common rating
        when(rateRepository.findByUserIdInAndVideoGameIdIn(
                List.of(viewer.getId(), target.getId()), commonIds))
                .thenReturn(List.of(rate(viewer, g1, 9)));

        ProfileComparisonDto result = service.compare(VIEWER_EMAIL, TARGET_USERNAME, PAGEABLE);

        // libraryScore = 1/3 * 100 = 33.33 -> round = 33 (rating component omitted)
        assertThat(result.affinityScore()).isEqualTo(33);
        CommonGameEntryDto entry = result.commonGames().content().get(0);
        assertThat(entry.viewerRating()).isEqualTo(4.5);
        assertThat(entry.targetRating()).isNull();
        assertThat(entry.ratingDiff()).isNull();
    }

    @Test
    @DisplayName("should return score 0 when both libraries are empty (union guard)")
    void compare_emptyLibraries_unionZero_scoreZero() {
        stubUsersFound();
        when(userGameRepository.findCommonVideoGameIds(viewer.getId(), target.getId()))
                .thenReturn(List.of());
        when(userGameRepository.countDistinctGamesByUserIds(viewer.getId(), target.getId()))
                .thenReturn(0L);
        when(userGameRepository.countByUserId(viewer.getId())).thenReturn(0L);
        when(userGameRepository.countByUserId(target.getId())).thenReturn(0L);

        ProfileComparisonDto result = service.compare(VIEWER_EMAIL, TARGET_USERNAME, PAGEABLE);

        assertThat(result.affinityScore()).isZero();
    }

    @Test
    @DisplayName("should sort common games by rating disagreement and paginate")
    void compare_sortsByRatingDiffDescNullsLast_andPaginates() {
        VideoGame g1 = game("Game A"); // diff 0.5
        VideoGame g2 = game("Game B"); // diff 3.0
        VideoGame g3 = game("Game C"); // diff null
        List<UUID> commonIds = List.of(g1.getId(), g2.getId(), g3.getId());

        stubUsersFound();
        when(userGameRepository.findCommonVideoGameIds(viewer.getId(), target.getId()))
                .thenReturn(commonIds);
        when(userGameRepository.countDistinctGamesByUserIds(viewer.getId(), target.getId()))
                .thenReturn(3L);
        when(userGameRepository.countByUserId(viewer.getId())).thenReturn(3L);
        when(userGameRepository.countByUserId(target.getId())).thenReturn(3L);
        when(userGameRepository.findByUserIdAndVideoGameIdIn(viewer.getId(), commonIds))
                .thenReturn(List.of(
                        userGame(viewer, g1, PlayStatus.COMPLETED),
                        userGame(viewer, g2, PlayStatus.COMPLETED),
                        userGame(viewer, g3, PlayStatus.COMPLETED)));
        when(userGameRepository.findByUserIdAndVideoGameIdIn(target.getId(), commonIds))
                .thenReturn(List.of(
                        userGame(target, g1, PlayStatus.COMPLETED),
                        userGame(target, g2, PlayStatus.COMPLETED),
                        userGame(target, g3, PlayStatus.COMPLETED)));
        when(rateRepository.findByUserIdInAndVideoGameIdIn(
                List.of(viewer.getId(), target.getId()), commonIds))
                .thenReturn(List.of(
                        rate(viewer, g1, 9), rate(target, g1, 8),   // 4.5 vs 4.0 -> 0.5
                        rate(viewer, g2, 10), rate(target, g2, 4),  // 5.0 vs 2.0 -> 3.0
                        rate(viewer, g3, 6)));                       // 3.0 vs null

        // Page 0 of size 2
        ProfileComparisonDto firstPage = service.compare(
                VIEWER_EMAIL, TARGET_USERNAME, PageRequest.of(0, 2));

        assertThat(firstPage.commonGames().metadata().totalElements()).isEqualTo(3);
        assertThat(firstPage.commonGames().metadata().totalPages()).isEqualTo(2);
        List<CommonGameEntryDto> firstContent = firstPage.commonGames().content();
        assertThat(firstContent).hasSize(2);
        assertThat(firstContent.get(0).title()).isEqualTo("Game B"); // diff 3.0 first
        assertThat(firstContent.get(1).title()).isEqualTo("Game A"); // diff 0.5

        // Page 1 of size 2 -> the null-diff game last
        ProfileComparisonDto secondPage = service.compare(
                VIEWER_EMAIL, TARGET_USERNAME, PageRequest.of(1, 2));
        List<CommonGameEntryDto> secondContent = secondPage.commonGames().content();
        assertThat(secondContent).hasSize(1);
        assertThat(secondContent.get(0).title()).isEqualTo("Game C");
        assertThat(secondContent.get(0).ratingDiff()).isNull();
    }

    // Helpers

    private void stubUsersFound() {
        when(userRepository.findByEmail(VIEWER_EMAIL)).thenReturn(Optional.of(viewer));
        when(userRepository.findByPseudo(TARGET_USERNAME)).thenReturn(Optional.of(target));
    }

    private static VideoGame game(String title) {
        VideoGame game = new VideoGame();
        game.setId(UUID.randomUUID());
        game.setTitle(title);
        game.setCoverUrl("/covers/" + title.replace(' ', '-') + ".png");
        game.setReleaseDate(LocalDate.of(2020, 1, 1));
        return game;
    }

    private static UserGame userGame(User user, VideoGame game, PlayStatus status) {
        return new UserGame(user, game, status);
    }

    private static Rate rate(User user, VideoGame game, int score) {
        return new Rate(user, game, score);
    }
}
