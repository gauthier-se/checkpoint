package com.checkpoint.api.services.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.checkpoint.api.dto.catalog.ReviewCardDto;
import com.checkpoint.api.dto.collection.BacklogResponseDto;
import com.checkpoint.api.dto.collection.LikedGameResponseDto;
import com.checkpoint.api.dto.collection.UnifiedGameResponseDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.dto.collection.WishResponseDto;
import com.checkpoint.api.dto.list.GameListCardDto;
import com.checkpoint.api.dto.onboarding.OnboardingSteps;
import com.checkpoint.api.dto.playlog.GamePlayLogResponseDto;
import com.checkpoint.api.dto.profile.ProfileUpdatedDto;
import com.checkpoint.api.dto.profile.RatingDistributionEntryDto;
import com.checkpoint.api.dto.profile.RecentPlayDto;
import com.checkpoint.api.dto.profile.UpdateProfileDto;
import com.checkpoint.api.dto.profile.UserProfileDto;
import com.checkpoint.api.entities.Backlog;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.enums.Priority;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.exceptions.ProfilePrivateException;
import com.checkpoint.api.exceptions.PseudoAlreadyExistsException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.mapper.BacklogMapper;
import com.checkpoint.api.mapper.GamePlayLogMapper;
import com.checkpoint.api.mapper.LikedGameMapper;
import com.checkpoint.api.mapper.ProfileMapper;
import com.checkpoint.api.mapper.ReviewMapper;
import com.checkpoint.api.mapper.UserGameMapper;
import com.checkpoint.api.mapper.WishMapper;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.BadgeRepository;
import com.checkpoint.api.repositories.CommentRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.GameListService;
import com.checkpoint.api.services.OnboardingService;
import com.checkpoint.api.services.ProfileService;
import com.checkpoint.api.services.StorageService;

/**
 * Implementation of {@link ProfileService}.
 * Provides public user profile data with privacy enforcement.
 */
@Service
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private static final String PROFILE_PICTURES_DIR = "profiles";
    private static final int RECENT_PLAYS_LIMIT = 5;

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final WishRepository wishRepository;
    private final UserGamePlayRepository userGamePlayRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final RateRepository rateRepository;
    private final UserGameRepository userGameRepository;
    private final BacklogRepository backlogRepository;
    private final BadgeRepository badgeRepository;
    private final GameListService gameListService;
    private final StorageService storageService;
    private final ProfileMapper profileMapper;
    private final ReviewMapper reviewMapper;
    private final WishMapper wishMapper;
    private final LikedGameMapper likedGameMapper;
    private final UserGameMapper userGameMapper;
    private final BacklogMapper backlogMapper;
    private final GamePlayLogMapper gamePlayLogMapper;
    private final OnboardingService onboardingService;

    /**
     * Constructs a new ProfileServiceImpl.
     */
    public ProfileServiceImpl(UserRepository userRepository,
                               ReviewRepository reviewRepository,
                               WishRepository wishRepository,
                               UserGamePlayRepository userGamePlayRepository,
                               LikeRepository likeRepository,
                               CommentRepository commentRepository,
                               RateRepository rateRepository,
                               UserGameRepository userGameRepository,
                               BacklogRepository backlogRepository,
                               BadgeRepository badgeRepository,
                               GameListService gameListService,
                               StorageService storageService,
                               ProfileMapper profileMapper,
                               ReviewMapper reviewMapper,
                               WishMapper wishMapper,
                               LikedGameMapper likedGameMapper,
                               UserGameMapper userGameMapper,
                               BacklogMapper backlogMapper,
                               GamePlayLogMapper gamePlayLogMapper,
                               OnboardingService onboardingService) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.wishRepository = wishRepository;
        this.userGamePlayRepository = userGamePlayRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.rateRepository = rateRepository;
        this.userGameRepository = userGameRepository;
        this.backlogRepository = backlogRepository;
        this.badgeRepository = badgeRepository;
        this.gameListService = gameListService;
        this.storageService = storageService;
        this.profileMapper = profileMapper;
        this.reviewMapper = reviewMapper;
        this.wishMapper = wishMapper;
        this.likedGameMapper = likedGameMapper;
        this.userGameMapper = userGameMapper;
        this.backlogMapper = backlogMapper;
        this.gamePlayLogMapper = gamePlayLogMapper;
        this.onboardingService = onboardingService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserProfileDto getUserProfile(String username, String viewerEmail) {
        log.info("Fetching profile for user: {}", username);

        User user = userRepository.findByPseudoWithBadgesAndFavorites(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        Long followerCount = userRepository.countFollowersByUserId(user.getId());
        Long followingCount = userRepository.countFollowingByUserId(user.getId());
        Long reviewCount = reviewRepository.countByUserPseudo(username);
        Long wishlistCount = wishRepository.countByUserPseudo(username);

        Boolean isFollowing = null;
        Boolean isOwner = false;

        if (viewerEmail != null) {
            User viewer = userRepository.findByEmail(viewerEmail).orElse(null);
            if (viewer != null) {
                isOwner = viewer.getId().equals(user.getId());
                isFollowing = userRepository.isFollowing(viewer.getId(), user.getId());
            }
        }

        List<RecentPlayDto> recentPlays = buildRecentPlays(user, isOwner);
        List<com.checkpoint.api.entities.Badge> badgeCatalog = badgeRepository.findAll();
        List<RatingDistributionEntryDto> ratingDistribution =
                rateRepository.findDistributionByUserId(user.getId());

        return profileMapper.toUserProfileDto(
                user, badgeCatalog, recentPlays, followerCount, followingCount,
                reviewCount, wishlistCount, ratingDistribution, isFollowing, isOwner);
    }

    /**
     * Builds the 5 most recent play projections for a user. Returns an empty list when the
     * profile is private and the viewer is not the owner (the client uses this to hide the
     * section without leaking additional signal beyond the existing {@code isPrivate} flag).
     * Likes are batch-resolved with a single {@code findVideoGameIdsLikedByUser} query.
     *
     * @param user    the profile owner (with associations already loaded)
     * @param isOwner whether the viewer is the profile owner
     * @return the compact recent plays, possibly empty
     */
    private List<RecentPlayDto> buildRecentPlays(User user, boolean isOwner) {
        if (Boolean.TRUE.equals(user.getIsPrivate()) && !isOwner) {
            return List.of();
        }

        List<UserGamePlay> plays = userGamePlayRepository.findRecentByUserId(
                user.getId(), PageRequest.of(0, RECENT_PLAYS_LIMIT));

        if (plays.isEmpty()) {
            return List.of();
        }

        List<UUID> gameIds = plays.stream()
                .map(p -> p.getVideoGame().getId())
                .toList();

        Set<UUID> likedGameIds = new HashSet<>(
                likeRepository.findVideoGameIdsLikedByUser(user.getId(), gameIds));

        return plays.stream()
                .map(p -> profileMapper.toRecentPlayDto(p, likedGameIds.contains(p.getVideoGame().getId())))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ReviewCardDto> getUserReviews(String username, String viewerEmail, Pageable pageable) {
        log.info("Fetching reviews for user: {}", username);

        User user = userRepository.findByPseudo(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        enforcePrivacy(user, viewerEmail);

        User viewer = viewerEmail != null
                ? userRepository.findByEmail(viewerEmail).orElse(null)
                : null;

        return reviewRepository.findByUserPseudo(username, pageable)
                .map(review -> {
                    long likesCount = likeRepository.countByReviewId(review.getId());
                    boolean hasLiked = viewer != null
                            && likeRepository.existsByUserIdAndReviewId(viewer.getId(), review.getId());
                    long commentsCount = commentRepository.countByReviewId(review.getId());
                    return reviewMapper.toCardDto(review, likesCount, hasLiked, commentsCount);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<WishResponseDto> getUserWishlist(String username, String viewerEmail, Pageable pageable) {
        log.info("Fetching wishlist for user: {}", username);

        User user = userRepository.findByPseudo(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        enforcePrivacy(user, viewerEmail);

        return wishRepository.findByUserPseudoWithVideoGame(username, pageable)
                .map(wishMapper::toResponseDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<LikedGameResponseDto> getUserLikedGames(String username, String viewerEmail, Pageable pageable) {
        log.info("Fetching liked games for user: {}", username);

        User user = userRepository.findByPseudo(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        enforcePrivacy(user, viewerEmail);

        return likeRepository.findGameLikesByUserPseudo(username, pageable)
                .map(likedGameMapper::toResponseDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<UserGameResponseDto> getUserLibrary(String username, String viewerEmail, PlayStatus status, Pageable pageable) {
        log.info("Fetching library for user: {} - status: {}", username, status);

        User user = userRepository.findByPseudo(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        enforcePrivacy(user, viewerEmail);

        return userGameRepository.findLibraryProjection(user.getId(), status, pageable)
                .map(row -> userGameMapper.toResponseDto((UserGame) row[0], (Integer) row[1]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<BacklogResponseDto> getUserBacklog(String username, String viewerEmail, Pageable pageable) {
        log.info("Fetching backlog for user: {}", username);

        User user = userRepository.findByPseudo(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        enforcePrivacy(user, viewerEmail);

        // Priority sorting is handled by dedicated queries (the JPQL CASE ordering cannot be
        // expressed through Pageable's Sort); fall back to the default date sort otherwise.
        Sort.Order priorityOrder = pageable.getSort().getOrderFor("priority");
        if (priorityOrder != null) {
            Pageable unsortedPage = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            Page<Backlog> page = priorityOrder.isAscending()
                    ? backlogRepository.findByUserIdWithVideoGameOrderByPriorityAsc(user.getId(), unsortedPage)
                    : backlogRepository.findByUserIdWithVideoGameOrderByPriorityDesc(user.getId(), unsortedPage);
            return page.map(backlogMapper::toResponseDto);
        }

        return backlogRepository.findByUserIdWithVideoGame(user.getId(), pageable)
                .map(backlogMapper::toResponseDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<GamePlayLogResponseDto> getUserPlayLog(String username, String viewerEmail, Pageable pageable) {
        log.info("Fetching play log for user: {}", username);

        User user = userRepository.findByPseudo(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        enforcePrivacy(user, viewerEmail);

        return userGamePlayRepository.findByUserId(user.getId(), pageable)
                .map(gamePlayLogMapper::toDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<UnifiedGameResponseDto> getUserAllGames(String username, String viewerEmail, Pageable pageable) {
        log.info("Fetching all games for user: {}", username);

        User user = userRepository.findByPseudo(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        enforcePrivacy(user, viewerEmail);

        // Intermediate flat record: one entry per (game, collectionType) pair
        record FlatEntry(UUID videoGameId, String title, String coverUrl, LocalDate releaseDate,
                         String collectionType, java.time.LocalDateTime addedAt,
                         PlayStatus libraryStatus, Double userRating, Priority priority) {}

        Pageable all = Pageable.unpaged();
        List<FlatEntry> flat = new ArrayList<>();

        // Library entries (includes rating)
        userGameRepository.findLibraryProjection(user.getId(), null, all)
                .getContent().forEach(row -> {
                    UserGame ug = (UserGame) row[0];
                    Integer rawScore = (Integer) row[1];
                    Double rating = rawScore != null ? rawScore / 2.0 : null;
                    flat.add(new FlatEntry(
                            ug.getVideoGame().getId(),
                            ug.getVideoGame().getTitle(),
                            ug.getVideoGame().getCoverUrl(),
                            ug.getVideoGame().getReleaseDate(),
                            "LIBRARY", ug.getCreatedAt(),
                            ug.getStatus(), rating, null
                    ));
                });

        // Wishlist entries
        wishRepository.findByUserPseudoWithVideoGame(username, all)
                .getContent().forEach(wish -> flat.add(new FlatEntry(
                        wish.getVideoGame().getId(),
                        wish.getVideoGame().getTitle(),
                        wish.getVideoGame().getCoverUrl(),
                        wish.getVideoGame().getReleaseDate(),
                        "WISHLIST", wish.getCreatedAt(),
                        null, null, wish.getPriority()
                )));

        // Backlog entries
        backlogRepository.findByUserIdWithVideoGame(user.getId(), all)
                .getContent().forEach(backlog -> flat.add(new FlatEntry(
                        backlog.getVideoGame().getId(),
                        backlog.getVideoGame().getTitle(),
                        backlog.getVideoGame().getCoverUrl(),
                        backlog.getVideoGame().getReleaseDate(),
                        "BACKLOG", backlog.getCreatedAt(),
                        null, null, backlog.getPriority()
                )));

        // Liked entries
        likeRepository.findGameLikesByUserPseudo(username, all)
                .getContent().forEach(like -> flat.add(new FlatEntry(
                        like.getVideoGame().getId(),
                        like.getVideoGame().getTitle(),
                        like.getVideoGame().getCoverUrl(),
                        like.getVideoGame().getReleaseDate(),
                        "LIKED", like.getCreatedAt(),
                        null, null, null
                )));

        // Deduplicate by videoGameId: merge collection types, keep richest metadata
        Map<UUID, List<FlatEntry>> byGameId = flat.stream()
                .collect(Collectors.groupingBy(FlatEntry::videoGameId, LinkedHashMap::new, Collectors.toList()));

        List<UnifiedGameResponseDto> deduped = byGameId.values().stream()
                .map(entries -> {
                    java.time.LocalDateTime latestAddedAt = entries.stream()
                            .map(FlatEntry::addedAt)
                            .max(Comparator.naturalOrder())
                            .orElseThrow();
                    List<String> collectionTypes = entries.stream()
                            .map(FlatEntry::collectionType)
                            .distinct()
                            .toList();
                    FlatEntry base = entries.get(0);
                    PlayStatus libraryStatus = entries.stream()
                            .filter(e -> "LIBRARY".equals(e.collectionType()) && e.libraryStatus() != null)
                            .map(FlatEntry::libraryStatus)
                            .findFirst().orElse(null);
                    Double userRating = entries.stream()
                            .filter(e -> "LIBRARY".equals(e.collectionType()) && e.userRating() != null)
                            .map(FlatEntry::userRating)
                            .findFirst().orElse(null);
                    Priority priority = entries.stream()
                            .filter(e -> e.priority() != null)
                            .map(FlatEntry::priority)
                            .findFirst().orElse(null);
                    return new UnifiedGameResponseDto(
                            base.videoGameId(), base.title(), base.coverUrl(),
                            base.releaseDate(), collectionTypes, latestAddedAt,
                            libraryStatus, userRating, priority
                    );
                })
                .sorted(Comparator.comparing(UnifiedGameResponseDto::addedAt).reversed())
                .toList();

        // Apply manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), deduped.size());
        List<UnifiedGameResponseDto> pageContent = start >= deduped.size()
                ? List.of() : deduped.subList(start, end);

        return new PageImpl<>(pageContent, pageable, deduped.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<GameListCardDto> getUserLists(String username, Pageable pageable) {
        log.info("Fetching game lists for user: {}", username);

        // Verify user exists
        userRepository.findByPseudo(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        return gameListService.getUserPublicLists(username, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProfileUpdatedDto updateProfile(String email, UpdateProfileDto dto) {
        log.info("Updating profile for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Check pseudo uniqueness (excluding current user)
        if (!user.getPseudo().equals(dto.pseudo())) {
            userRepository.findByPseudo(dto.pseudo()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new PseudoAlreadyExistsException(dto.pseudo());
                }
            });
            user.setPseudo(dto.pseudo());
        }

        user.setBio(dto.bio());

        if (dto.isPrivate() != null) {
            user.setIsPrivate(dto.isPrivate());
        }

        User savedUser = userRepository.save(user);
        if (savedUser.getBio() != null && !savedUser.getBio().isBlank()) {
            onboardingService.markStepDone(email, OnboardingSteps.BIO);
        }
        return profileMapper.toProfileUpdatedDto(savedUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String updatePicture(String email, MultipartFile file) {
        log.info("Updating profile picture for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Delete old picture if exists
        if (user.getPicture() != null) {
            String oldStoragePath = user.getPicture().replaceFirst("^/uploads/", "");
            storageService.delete(oldStoragePath);
        }

        String storagePath = storageService.store(file, PROFILE_PICTURES_DIR);
        String servingUrl = "/uploads/" + storagePath;
        user.setPicture(servingUrl);
        userRepository.save(user);

        onboardingService.markStepDone(email, OnboardingSteps.PICTURE);

        return servingUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deletePicture(String email) {
        log.info("Deleting profile picture for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (user.getPicture() != null) {
            String storagePath = user.getPicture().replaceFirst("^/uploads/", "");
            storageService.delete(storagePath);
            user.setPicture(null);
            userRepository.save(user);
        }
    }

    /**
     * Checks if the profile is private and the viewer is not the owner.
     * Throws {@link ProfilePrivateException} if access is denied.
     *
     * @param profileOwner the profile owner
     * @param viewerEmail  the viewer's email, or null if anonymous
     */
    private void enforcePrivacy(User profileOwner, String viewerEmail) {
        if (!Boolean.TRUE.equals(profileOwner.getIsPrivate())) {
            return;
        }

        if (viewerEmail != null) {
            User viewer = userRepository.findByEmail(viewerEmail).orElse(null);
            if (viewer != null && viewer.getId().equals(profileOwner.getId())) {
                return;
            }
        }

        throw new ProfilePrivateException(profileOwner.getPseudo());
    }
}
