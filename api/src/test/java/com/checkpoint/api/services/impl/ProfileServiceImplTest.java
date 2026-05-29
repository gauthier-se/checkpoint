package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.dto.collection.BacklogResponseDto;
import com.checkpoint.api.dto.collection.LikedGameResponseDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.dto.collection.WishResponseDto;
import com.checkpoint.api.dto.playlog.GamePlayLogResponseDto;
import com.checkpoint.api.dto.profile.RecentPlayDto;
import com.checkpoint.api.dto.profile.UserProfileDto;
import com.checkpoint.api.entities.Backlog;
import com.checkpoint.api.entities.Like;
import com.checkpoint.api.entities.Review;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.entities.Wish;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.enums.Priority;
import com.checkpoint.api.exceptions.ProfilePrivateException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.mapper.BacklogMapper;
import com.checkpoint.api.mapper.GamePlayLogMapper;
import com.checkpoint.api.mapper.LikedGameMapper;
import com.checkpoint.api.mapper.ProfileMapper;
import com.checkpoint.api.mapper.ReviewMapper;
import com.checkpoint.api.mapper.UserGameMapper;
import com.checkpoint.api.mapper.WishMapper;
import com.checkpoint.api.mapper.impl.ProfileMapperImpl;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.GameListService;
import com.checkpoint.api.services.StorageService;

/**
 * Unit tests for {@link ProfileServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private WishRepository wishRepository;

    @Mock
    private UserGamePlayRepository userGamePlayRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private BacklogRepository backlogRepository;

    @Mock
    private com.checkpoint.api.repositories.BadgeRepository badgeRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private WishMapper wishMapper;

    @Mock
    private LikedGameMapper likedGameMapper;

    @Mock
    private UserGameMapper userGameMapper;

    @Mock
    private BacklogMapper backlogMapper;

    @Mock
    private GamePlayLogMapper gamePlayLogMapper;

    @Mock
    private GameListService gameListService;

    @Mock
    private StorageService storageService;

    @Mock
    private com.checkpoint.api.services.OnboardingService onboardingService;

    private ProfileMapper profileMapper;
    private ProfileServiceImpl profileService;

    private User profileUser;
    private User viewerUser;

    @BeforeEach
    void setUp() {
        profileMapper = new ProfileMapperImpl();
        profileService = new ProfileServiceImpl(
                userRepository, reviewRepository, wishRepository,
                userGamePlayRepository, likeRepository,
                userGameRepository, backlogRepository, badgeRepository,
                gameListService, storageService, profileMapper, reviewMapper, wishMapper,
                likedGameMapper, userGameMapper, backlogMapper, gamePlayLogMapper, onboardingService);

        profileUser = new User();
        profileUser.setId(UUID.randomUUID());
        profileUser.setEmail("profile@example.com");
        profileUser.setPseudo("gamer123");
        profileUser.setBio("I love games");
        profileUser.setLevel(3);
        profileUser.setXpPoint(2500);
        profileUser.setIsPrivate(false);
        profileUser.setBadges(new HashSet<>());

        viewerUser = new User();
        viewerUser.setId(UUID.randomUUID());
        viewerUser.setEmail("viewer@example.com");
        viewerUser.setPseudo("viewer");

        // Default: no recent plays. Lenient so tests not exercising this path don't fail.
        lenient().when(userGamePlayRepository.findRecentByUserId(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {

        @Test
        @DisplayName("should return profile for public user without authentication")
        void getUserProfile_shouldReturnProfileForAnonymousViewer() {
            // Given
            when(userRepository.findByPseudoWithBadgesAndFavorites("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(userRepository.countFollowersByUserId(profileUser.getId())).thenReturn(10L);
            when(userRepository.countFollowingByUserId(profileUser.getId())).thenReturn(5L);
            when(reviewRepository.countByUserPseudo("gamer123")).thenReturn(3L);
            when(wishRepository.countByUserPseudo("gamer123")).thenReturn(7L);

            // When
            UserProfileDto result = profileService.getUserProfile("gamer123", null);

            // Then
            assertThat(result.username()).isEqualTo("gamer123");
            assertThat(result.bio()).isEqualTo("I love games");
            assertThat(result.level()).isEqualTo(3);
            assertThat(result.xpPoint()).isEqualTo(2500);
            assertThat(result.xpThreshold()).isEqualTo(3000);
            assertThat(result.followerCount()).isEqualTo(10L);
            assertThat(result.followingCount()).isEqualTo(5L);
            assertThat(result.reviewCount()).isEqualTo(3L);
            assertThat(result.wishlistCount()).isEqualTo(7L);
            assertThat(result.isFollowing()).isNull();
            assertThat(result.isOwner()).isFalse();
        }

        @Test
        @DisplayName("should compute isFollowing and isOwner when authenticated")
        void getUserProfile_shouldComputeFollowingAndOwnerWhenAuthenticated() {
            // Given
            when(userRepository.findByPseudoWithBadgesAndFavorites("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(userRepository.countFollowersByUserId(profileUser.getId())).thenReturn(0L);
            when(userRepository.countFollowingByUserId(profileUser.getId())).thenReturn(0L);
            when(reviewRepository.countByUserPseudo("gamer123")).thenReturn(0L);
            when(wishRepository.countByUserPseudo("gamer123")).thenReturn(0L);
            when(userRepository.findByEmail("viewer@example.com"))
                    .thenReturn(Optional.of(viewerUser));
            when(userRepository.isFollowing(viewerUser.getId(), profileUser.getId()))
                    .thenReturn(true);

            // When
            UserProfileDto result = profileService.getUserProfile("gamer123", "viewer@example.com");

            // Then
            assertThat(result.isFollowing()).isTrue();
            assertThat(result.isOwner()).isFalse();
        }

        @Test
        @DisplayName("should set isOwner to true when viewer is profile owner")
        void getUserProfile_shouldSetIsOwnerWhenViewerIsOwner() {
            // Given
            when(userRepository.findByPseudoWithBadgesAndFavorites("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(userRepository.countFollowersByUserId(profileUser.getId())).thenReturn(0L);
            when(userRepository.countFollowingByUserId(profileUser.getId())).thenReturn(0L);
            when(reviewRepository.countByUserPseudo("gamer123")).thenReturn(0L);
            when(wishRepository.countByUserPseudo("gamer123")).thenReturn(0L);
            when(userRepository.findByEmail("profile@example.com"))
                    .thenReturn(Optional.of(profileUser));
            when(userRepository.isFollowing(profileUser.getId(), profileUser.getId()))
                    .thenReturn(false);

            // When
            UserProfileDto result = profileService.getUserProfile("gamer123", "profile@example.com");

            // Then
            assertThat(result.isOwner()).isTrue();
        }

        @Test
        @DisplayName("should throw UserNotFoundException for unknown username")
        void getUserProfile_shouldThrowForUnknownUser() {
            // Given
            when(userRepository.findByPseudoWithBadgesAndFavorites("unknown"))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> profileService.getUserProfile("unknown", null))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("should populate recentPlays with correct flags and batched isLiked")
        void getUserProfile_shouldPopulateRecentPlaysWithBatchedLikes() {
            // Given: three plays — one scored, one with a review, one replay.
            VideoGame gameA = new VideoGame();
            gameA.setId(UUID.randomUUID());
            gameA.setTitle("Game A");
            gameA.setCoverUrl("/covers/a.png");

            VideoGame gameB = new VideoGame();
            gameB.setId(UUID.randomUUID());
            gameB.setTitle("Game B");
            gameB.setCoverUrl(null);

            VideoGame gameC = new VideoGame();
            gameC.setId(UUID.randomUUID());
            gameC.setTitle("Game C");
            gameC.setCoverUrl("/covers/c.png");

            UserGamePlay playA = new UserGamePlay();
            playA.setId(UUID.randomUUID());
            playA.setVideoGame(gameA);
            playA.setScore(8);
            playA.setIsReplay(false);
            playA.setCreatedAt(LocalDateTime.now());

            UserGamePlay playB = new UserGamePlay();
            playB.setId(UUID.randomUUID());
            playB.setVideoGame(gameB);
            playB.setScore(null);
            playB.setIsReplay(false);
            playB.setCreatedAt(LocalDateTime.now().minusHours(1));
            Review review = new Review();
            review.setId(UUID.randomUUID());
            playB.setReview(review);

            UserGamePlay playC = new UserGamePlay();
            playC.setId(UUID.randomUUID());
            playC.setVideoGame(gameC);
            playC.setScore(5);
            playC.setIsReplay(true);
            playC.setCreatedAt(LocalDateTime.now().minusHours(2));

            when(userRepository.findByPseudoWithBadgesAndFavorites("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(userRepository.countFollowersByUserId(profileUser.getId())).thenReturn(0L);
            when(userRepository.countFollowingByUserId(profileUser.getId())).thenReturn(0L);
            when(reviewRepository.countByUserPseudo("gamer123")).thenReturn(0L);
            when(wishRepository.countByUserPseudo("gamer123")).thenReturn(0L);
            when(userGamePlayRepository.findRecentByUserId(eq(profileUser.getId()), any(Pageable.class)))
                    .thenReturn(List.of(playA, playB, playC));
            // Only Game A is liked — the batched query returns just A's id.
            when(likeRepository.findVideoGameIdsLikedByUser(eq(profileUser.getId()), anyCollection()))
                    .thenReturn(List.of(gameA.getId()));

            // When
            UserProfileDto result = profileService.getUserProfile("gamer123", null);

            // Then
            assertThat(result.recentPlays()).hasSize(3);

            RecentPlayDto dtoA = result.recentPlays().get(0);
            assertThat(dtoA.id()).isEqualTo(playA.getId());
            assertThat(dtoA.videoGameId()).isEqualTo(gameA.getId());
            assertThat(dtoA.title()).isEqualTo("Game A");
            assertThat(dtoA.coverUrl()).isEqualTo("/covers/a.png");
            assertThat(dtoA.score()).isEqualTo(8);
            assertThat(dtoA.hasReview()).isFalse();
            assertThat(dtoA.isReplay()).isFalse();
            assertThat(dtoA.isLiked()).isTrue();

            RecentPlayDto dtoB = result.recentPlays().get(1);
            assertThat(dtoB.score()).isNull();
            assertThat(dtoB.hasReview()).isTrue();
            assertThat(dtoB.isReplay()).isFalse();
            assertThat(dtoB.isLiked()).isFalse();

            RecentPlayDto dtoC = result.recentPlays().get(2);
            assertThat(dtoC.score()).isEqualTo(5);
            assertThat(dtoC.hasReview()).isFalse();
            assertThat(dtoC.isReplay()).isTrue();
            assertThat(dtoC.isLiked()).isFalse();
        }

        @Test
        @DisplayName("should return empty recentPlays for private profile when viewer is not owner")
        void getUserProfile_shouldMaskRecentPlaysForPrivateProfile() {
            // Given
            profileUser.setIsPrivate(true);
            when(userRepository.findByPseudoWithBadgesAndFavorites("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(userRepository.countFollowersByUserId(profileUser.getId())).thenReturn(0L);
            when(userRepository.countFollowingByUserId(profileUser.getId())).thenReturn(0L);
            when(reviewRepository.countByUserPseudo("gamer123")).thenReturn(0L);
            when(wishRepository.countByUserPseudo("gamer123")).thenReturn(0L);
            when(userRepository.findByEmail("viewer@example.com"))
                    .thenReturn(Optional.of(viewerUser));
            when(userRepository.isFollowing(viewerUser.getId(), profileUser.getId()))
                    .thenReturn(false);

            // When
            UserProfileDto result = profileService.getUserProfile("gamer123", "viewer@example.com");

            // Then
            assertThat(result.isPrivate()).isTrue();
            assertThat(result.isOwner()).isFalse();
            assertThat(result.recentPlays()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserReviews")
    class GetUserReviews {

        @Test
        @DisplayName("should return reviews for public profile")
        void getUserReviews_shouldReturnReviewsForPublicProfile() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Review> reviewPage = new PageImpl<>(List.of());

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(reviewRepository.findByUserPseudo("gamer123", pageable))
                    .thenReturn(reviewPage);

            // When
            Page<ReviewResponseDto> result = profileService.getUserReviews("gamer123", null, pageable);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ProfilePrivateException for private profile when not owner")
        void getUserReviews_shouldThrowForPrivateProfile() {
            // Given
            profileUser.setIsPrivate(true);
            Pageable pageable = PageRequest.of(0, 20);

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));

            // When / Then
            assertThatThrownBy(() -> profileService.getUserReviews("gamer123", null, pageable))
                    .isInstanceOf(ProfilePrivateException.class);
        }

        @Test
        @DisplayName("should allow owner to view own private profile reviews")
        void getUserReviews_shouldAllowOwnerToViewPrivateReviews() {
            // Given
            profileUser.setIsPrivate(true);
            Pageable pageable = PageRequest.of(0, 20);
            Page<Review> reviewPage = new PageImpl<>(List.of());

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(userRepository.findByEmail("profile@example.com"))
                    .thenReturn(Optional.of(profileUser));
            when(reviewRepository.findByUserPseudo("gamer123", pageable))
                    .thenReturn(reviewPage);

            // When
            Page<ReviewResponseDto> result = profileService.getUserReviews(
                    "gamer123", "profile@example.com", pageable);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserWishlist")
    class GetUserWishlist {

        @Test
        @DisplayName("should return wishlist for public profile")
        void getUserWishlist_shouldReturnWishlistForPublicProfile() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Wish> wishPage = new PageImpl<>(List.of());

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(wishRepository.findByUserPseudoWithVideoGame("gamer123", pageable))
                    .thenReturn(wishPage);

            // When
            Page<WishResponseDto> result = profileService.getUserWishlist("gamer123", null, pageable);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ProfilePrivateException for private profile wishlist")
        void getUserWishlist_shouldThrowForPrivateProfile() {
            // Given
            profileUser.setIsPrivate(true);
            Pageable pageable = PageRequest.of(0, 20);

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));

            // When / Then
            assertThatThrownBy(() -> profileService.getUserWishlist("gamer123", null, pageable))
                    .isInstanceOf(ProfilePrivateException.class);
        }
    }

    @Nested
    @DisplayName("getUserLikedGames")
    class GetUserLikedGames {

        @Test
        @DisplayName("should return liked games for public profile")
        void getUserLikedGames_shouldReturnLikedGamesForPublicProfile() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            VideoGame game = new VideoGame();
            game.setId(UUID.randomUUID());
            game.setTitle("Hollow Knight");
            Like like = Like.forVideoGame(profileUser, game);
            like.setId(UUID.randomUUID());
            Page<Like> likePage = new PageImpl<>(List.of(like), pageable, 1);

            LikedGameResponseDto dto = new LikedGameResponseDto(
                    like.getId(), game.getId(), game.getTitle(), null, null, LocalDateTime.now());

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(likeRepository.findGameLikesByUserPseudo("gamer123", pageable))
                    .thenReturn(likePage);
            when(likedGameMapper.toResponseDto(like)).thenReturn(dto);

            // When
            Page<LikedGameResponseDto> result =
                    profileService.getUserLikedGames("gamer123", null, pageable);

            // Then
            assertThat(result.getContent()).containsExactly(dto);
        }

        @Test
        @DisplayName("should throw ProfilePrivateException for private profile liked games")
        void getUserLikedGames_shouldThrowForPrivateProfile() {
            // Given
            profileUser.setIsPrivate(true);
            Pageable pageable = PageRequest.of(0, 20);

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));

            // When / Then
            assertThatThrownBy(() -> profileService.getUserLikedGames("gamer123", null, pageable))
                    .isInstanceOf(ProfilePrivateException.class);
        }

        @Test
        @DisplayName("should throw UserNotFoundException for unknown username")
        void getUserLikedGames_shouldThrowForUnknownUser() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.findByPseudo("ghost"))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> profileService.getUserLikedGames("ghost", null, pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUserLibrary")
    class GetUserLibrary {

        @Test
        @DisplayName("should return library for public profile with rating populated")
        void getUserLibrary_shouldReturnLibraryForPublicProfile() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            UserGame userGame = new UserGame();
            userGame.setId(UUID.randomUUID());
            Page<Object[]> projection = new PageImpl<>(
                    List.<Object[]>of(new Object[] { userGame, 7 }), pageable, 1);

            UserGameResponseDto dto = new UserGameResponseDto(
                    userGame.getId(), UUID.randomUUID(), "Celeste", null, null,
                    PlayStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now(), null, 3.5);

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(userGameRepository.findLibraryProjection(profileUser.getId(), null, pageable))
                    .thenReturn(projection);
            when(userGameMapper.toResponseDto(userGame, 7)).thenReturn(dto);

            // When
            Page<UserGameResponseDto> result =
                    profileService.getUserLibrary("gamer123", null, null, pageable);

            // Then
            assertThat(result.getContent()).containsExactly(dto);
        }

        @Test
        @DisplayName("should pass status filter to the repository")
        void getUserLibrary_shouldFilterByStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            UserGame userGame = new UserGame();
            userGame.setId(UUID.randomUUID());
            Page<Object[]> projection = new PageImpl<>(
                    List.<Object[]>of(new Object[] { userGame, null }), pageable, 1);

            UserGameResponseDto dto = new UserGameResponseDto(
                    userGame.getId(), UUID.randomUUID(), "Hades", null, null,
                    PlayStatus.ARE_PLAYING, LocalDateTime.now(), LocalDateTime.now(), null, null);

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(userGameRepository.findLibraryProjection(profileUser.getId(), PlayStatus.ARE_PLAYING, pageable))
                    .thenReturn(projection);
            when(userGameMapper.toResponseDto(userGame, null)).thenReturn(dto);

            // When
            profileService.getUserLibrary("gamer123", null, PlayStatus.ARE_PLAYING, pageable);

            // Then
            verify(userGameRepository).findLibraryProjection(profileUser.getId(), PlayStatus.ARE_PLAYING, pageable);
        }

        @Test
        @DisplayName("should throw ProfilePrivateException for private profile library")
        void getUserLibrary_shouldThrowForPrivateProfile() {
            // Given
            profileUser.setIsPrivate(true);
            Pageable pageable = PageRequest.of(0, 20);

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));

            // When / Then
            assertThatThrownBy(() -> profileService.getUserLibrary("gamer123", null, null, pageable))
                    .isInstanceOf(ProfilePrivateException.class);
        }

        @Test
        @DisplayName("should throw UserNotFoundException for unknown username")
        void getUserLibrary_shouldThrowForUnknownUser() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.findByPseudo("ghost"))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> profileService.getUserLibrary("ghost", null, null, pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUserBacklog")
    class GetUserBacklog {

        @Test
        @DisplayName("should return backlog for public profile with default sort")
        void getUserBacklog_shouldReturnBacklogForPublicProfile() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Backlog backlog = new Backlog();
            backlog.setId(UUID.randomUUID());
            Page<Backlog> backlogPage = new PageImpl<>(List.of(backlog), pageable, 1);

            BacklogResponseDto dto = new BacklogResponseDto(
                    backlog.getId(), UUID.randomUUID(), "Hades", null, null,
                    Priority.HIGH, LocalDateTime.now());

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(backlogRepository.findByUserIdWithVideoGame(profileUser.getId(), pageable))
                    .thenReturn(backlogPage);
            when(backlogMapper.toResponseDto(backlog)).thenReturn(dto);

            // When
            Page<BacklogResponseDto> result =
                    profileService.getUserBacklog("gamer123", null, pageable);

            // Then
            assertThat(result.getContent()).containsExactly(dto);
        }

        @Test
        @DisplayName("should use priority-ordered query when sorting by priority")
        void getUserBacklog_shouldUsePriorityQueryWhenSortingByPriority() {
            // Given
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "priority"));
            Backlog backlog = new Backlog();
            backlog.setId(UUID.randomUUID());
            Page<Backlog> backlogPage = new PageImpl<>(List.of(backlog), PageRequest.of(0, 20), 1);

            BacklogResponseDto dto = new BacklogResponseDto(
                    backlog.getId(), UUID.randomUUID(), "Hades", null, null,
                    Priority.HIGH, LocalDateTime.now());

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(backlogRepository.findByUserIdWithVideoGameOrderByPriorityDesc(
                    eq(profileUser.getId()), any(Pageable.class)))
                    .thenReturn(backlogPage);
            when(backlogMapper.toResponseDto(backlog)).thenReturn(dto);

            // When
            Page<BacklogResponseDto> result =
                    profileService.getUserBacklog("gamer123", null, pageable);

            // Then
            assertThat(result.getContent()).containsExactly(dto);
        }

        @Test
        @DisplayName("should throw ProfilePrivateException for private profile backlog")
        void getUserBacklog_shouldThrowForPrivateProfile() {
            // Given
            profileUser.setIsPrivate(true);
            Pageable pageable = PageRequest.of(0, 20);

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));

            // When / Then
            assertThatThrownBy(() -> profileService.getUserBacklog("gamer123", null, pageable))
                    .isInstanceOf(ProfilePrivateException.class);
        }
    }

    @Nested
    @DisplayName("getUserPlayLog")
    class GetUserPlayLog {

        @Test
        @DisplayName("should return play log for public profile")
        void getUserPlayLog_shouldReturnPlayLogForPublicProfile() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            UserGamePlay play = new UserGamePlay();
            play.setId(UUID.randomUUID());
            Page<UserGamePlay> playPage = new PageImpl<>(List.of(play), pageable, 1);

            GamePlayLogResponseDto dto = new GamePlayLogResponseDto(
                    play.getId(), UUID.randomUUID(), "Stardew Valley", null, null, null, null,
                    PlayStatus.COMPLETED, false, 1200, null, null, null,
                    LocalDateTime.now(), LocalDateTime.now(), false, null, null, List.of());

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));
            when(userGamePlayRepository.findByUserId(profileUser.getId(), pageable))
                    .thenReturn(playPage);
            when(gamePlayLogMapper.toDto(play)).thenReturn(dto);

            // When
            Page<GamePlayLogResponseDto> result =
                    profileService.getUserPlayLog("gamer123", null, pageable);

            // Then
            assertThat(result.getContent()).containsExactly(dto);
        }

        @Test
        @DisplayName("should throw ProfilePrivateException for private profile play log")
        void getUserPlayLog_shouldThrowForPrivateProfile() {
            // Given
            profileUser.setIsPrivate(true);
            Pageable pageable = PageRequest.of(0, 20);

            when(userRepository.findByPseudo("gamer123"))
                    .thenReturn(Optional.of(profileUser));

            // When / Then
            assertThatThrownBy(() -> profileService.getUserPlayLog("gamer123", null, pageable))
                    .isInstanceOf(ProfilePrivateException.class);
        }

        @Test
        @DisplayName("should throw UserNotFoundException for unknown username")
        void getUserPlayLog_shouldThrowForUnknownUser() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.findByPseudo("ghost"))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> profileService.getUserPlayLog("ghost", null, pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
