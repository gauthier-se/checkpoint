package com.checkpoint.api.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.social.FriendActivityEntryDto;
import com.checkpoint.api.dto.social.FriendGameActivityDto;
import com.checkpoint.api.dto.social.FriendWantToPlayDto;
import com.checkpoint.api.dto.social.FriendWishlistEntryDto;
import com.checkpoint.api.entities.Backlog;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGamePlay;
import com.checkpoint.api.entities.Wish;
import com.checkpoint.api.enums.FriendCollectionType;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.GameSocialService;

/**
 * Default implementation of {@link GameSocialService}. Reads the viewer's
 * following set directly off the {@link User} entity (the {@code user_follows}
 * join table) and joins per-game plays/rates/wishlists/backlogs with one query
 * each to avoid N+1.
 */
@Service
@Transactional(readOnly = true)
public class GameSocialServiceImpl implements GameSocialService {

    private static final Logger log = LoggerFactory.getLogger(GameSocialServiceImpl.class);

    private final UserRepository userRepository;
    private final VideoGameRepository videoGameRepository;
    private final UserGamePlayRepository userGamePlayRepository;
    private final RateRepository rateRepository;
    private final WishRepository wishRepository;
    private final BacklogRepository backlogRepository;

    public GameSocialServiceImpl(UserRepository userRepository,
                                 VideoGameRepository videoGameRepository,
                                 UserGamePlayRepository userGamePlayRepository,
                                 RateRepository rateRepository,
                                 WishRepository wishRepository,
                                 BacklogRepository backlogRepository) {
        this.userRepository = userRepository;
        this.videoGameRepository = videoGameRepository;
        this.userGamePlayRepository = userGamePlayRepository;
        this.rateRepository = rateRepository;
        this.wishRepository = wishRepository;
        this.backlogRepository = backlogRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FriendGameActivityDto getFriendsActivity(UUID videoGameId, String viewerEmail) {
        if (!videoGameRepository.existsById(videoGameId)) {
            throw new GameNotFoundException(videoGameId);
        }

        List<UUID> followingIds = resolveFollowingIds(viewerEmail);
        if (followingIds.isEmpty()) {
            return new FriendGameActivityDto(0, Collections.emptyMap(), Collections.emptyList());
        }

        List<UserGamePlay> latestPlays = userGamePlayRepository
                .findLatestPerUserForGame(videoGameId, followingIds);
        List<Rate> rates = rateRepository.findByVideoGameIdAndUserIdIn(videoGameId, followingIds);

        // Index rates by user for O(1) lookup when assembling entries.
        Map<UUID, Integer> scoreByUserId = new HashMap<>(rates.size());
        for (Rate rate : rates) {
            scoreByUserId.put(rate.getUser().getId(), rate.getScore());
        }

        // Friends who only rated (no play) deserve an entry too — collect them.
        Map<UUID, FriendActivityEntryDto> entriesByUserId = new HashMap<>();
        for (UserGamePlay play : latestPlays) {
            User u = play.getUser();
            Integer score = scoreByUserId.get(u.getId());
            entriesByUserId.put(u.getId(), new FriendActivityEntryDto(
                    u.getId(),
                    u.getPseudo(),
                    u.getPicture(),
                    play.getStatus(),
                    score != null ? score / 2.0 : null,
                    play.getReview() != null,
                    play.getId()
            ));
        }
        for (Rate rate : rates) {
            User u = rate.getUser();
            entriesByUserId.computeIfAbsent(u.getId(), id -> new FriendActivityEntryDto(
                    u.getId(),
                    u.getPseudo(),
                    u.getPicture(),
                    null,
                    rate.getScore() / 2.0,
                    false,
                    null
            ));
        }

        if (entriesByUserId.isEmpty()) {
            return new FriendGameActivityDto(0, Collections.emptyMap(), Collections.emptyList());
        }

        // Per-status counts (only for friends with a primary play status).
        Map<PlayStatus, Long> countsByPlayStatus = new EnumMap<>(PlayStatus.class);
        for (FriendActivityEntryDto entry : entriesByUserId.values()) {
            if (entry.primaryPlayStatus() != null) {
                countsByPlayStatus.merge(entry.primaryPlayStatus(), 1L, Long::sum);
            }
        }

        // Stable ordering: friends with a play first (most recent first), then rated-only.
        Map<UUID, Integer> playIndex = new HashMap<>();
        for (int i = 0; i < latestPlays.size(); i++) {
            playIndex.put(latestPlays.get(i).getUser().getId(), i);
        }
        Comparator<FriendActivityEntryDto> orderByLatestPlayThenUserId = (a, b) -> {
            Integer ai = playIndex.get(a.userId());
            Integer bi = playIndex.get(b.userId());
            if (ai != null && bi != null) {
                UserGamePlay pa = latestPlays.get(ai);
                UserGamePlay pb = latestPlays.get(bi);
                int byDate = pb.getCreatedAt().compareTo(pa.getCreatedAt());
                if (byDate != 0) return byDate;
            } else if (ai != null) {
                return -1;
            } else if (bi != null) {
                return 1;
            }
            return a.userId().compareTo(b.userId());
        };
        List<FriendActivityEntryDto> ordered = new ArrayList<>(entriesByUserId.values());
        ordered.sort(orderByLatestPlayThenUserId);

        return new FriendGameActivityDto(ordered.size(), countsByPlayStatus, ordered);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FriendWantToPlayDto getFriendsWantToPlay(UUID videoGameId, String viewerEmail) {
        if (!videoGameRepository.existsById(videoGameId)) {
            throw new GameNotFoundException(videoGameId);
        }

        List<UUID> followingIds = resolveFollowingIds(viewerEmail);
        if (followingIds.isEmpty()) {
            return new FriendWantToPlayDto(0, 0L, 0L, Collections.emptyList());
        }

        List<Wish> wishes = wishRepository.findByVideoGameIdAndUserIdIn(videoGameId, followingIds);
        List<Backlog> backlogs = backlogRepository.findByVideoGameIdAndUserIdIn(videoGameId, followingIds);

        // A friend can theoretically have the game in both — prefer WISHLIST in that case
        // (it's a stronger "want" signal than a backlog entry).
        Map<UUID, FriendWishlistEntryDto> entriesByUserId = new HashMap<>();
        for (Wish wish : wishes) {
            User u = wish.getUser();
            entriesByUserId.put(u.getId(), new FriendWishlistEntryDto(
                    u.getId(), u.getPseudo(), u.getPicture(), FriendCollectionType.WISHLIST));
        }
        for (Backlog backlog : backlogs) {
            User u = backlog.getUser();
            entriesByUserId.computeIfAbsent(u.getId(), id -> new FriendWishlistEntryDto(
                    u.getId(), u.getPseudo(), u.getPicture(), FriendCollectionType.BACKLOG));
        }

        long wishlistCount = entriesByUserId.values().stream()
                .filter(e -> e.collectionType() == FriendCollectionType.WISHLIST)
                .count();
        long backlogCount = entriesByUserId.values().stream()
                .filter(e -> e.collectionType() == FriendCollectionType.BACKLOG)
                .count();

        // Stable ordering: wishlist first, then backlog; within each, by pseudo.
        List<FriendWishlistEntryDto> ordered = entriesByUserId.values().stream()
                .sorted(Comparator
                        .comparing(FriendWishlistEntryDto::collectionType)
                        .thenComparing(FriendWishlistEntryDto::pseudo, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        return new FriendWantToPlayDto(ordered.size(), wishlistCount, backlogCount, ordered);
    }

    /**
     * Resolves the IDs of users the viewer follows. Returns an empty list when
     * the viewer is anonymous, unknown, or follows nobody.
     */
    private List<UUID> resolveFollowingIds(String viewerEmail) {
        if (viewerEmail == null) {
            return Collections.emptyList();
        }
        User viewer = userRepository.findByEmail(viewerEmail).orElse(null);
        if (viewer == null) {
            log.debug("Unknown viewer email '{}' — returning empty friend payload", viewerEmail);
            return Collections.emptyList();
        }
        Set<User> following = viewer.getFollowing();
        if (following == null || following.isEmpty()) {
            return Collections.emptyList();
        }
        return following.stream().map(User::getId).toList();
    }
}
