package com.checkpoint.api.services.impl;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.onboarding.OnboardingSteps;
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
import com.checkpoint.api.services.FavoriteService;
import com.checkpoint.api.services.OnboardingService;

/**
 * Implementation of {@link FavoriteService}.
 */
@Service
@Transactional(readOnly = true)
public class FavoriteServiceImpl implements FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteServiceImpl.class);

    private static final int MAX_FAVORITES = 5;

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final VideoGameRepository videoGameRepository;
    private final ProfileMapper profileMapper;
    private final OnboardingService onboardingService;

    /**
     * Constructs a new FavoriteServiceImpl.
     *
     * @param favoriteRepository  the favorite repository
     * @param userRepository      the user repository
     * @param videoGameRepository the video game repository
     * @param profileMapper       the profile mapper
     * @param onboardingService   the onboarding service
     */
    public FavoriteServiceImpl(FavoriteRepository favoriteRepository,
                                UserRepository userRepository,
                                VideoGameRepository videoGameRepository,
                                ProfileMapper profileMapper,
                                OnboardingService onboardingService) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.videoGameRepository = videoGameRepository;
        this.profileMapper = profileMapper;
        this.onboardingService = onboardingService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FavoriteDto> getFavorites(UUID userId) {
        log.debug("Fetching favorites for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        return favoriteRepository.findByUserOrderByDisplayOrderAsc(user).stream()
                .map(profileMapper::toFavoriteDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<FavoriteDto> replaceFavorites(String email, List<UUID> orderedGameIds) {
        log.info("Replacing favorites for user: {} (count: {})", email, orderedGameIds.size());

        if (orderedGameIds.size() > MAX_FAVORITES) {
            throw new InvalidFavoritesException(
                    "A user can have at most " + MAX_FAVORITES + " favorite games");
        }
        if (new HashSet<>(orderedGameIds).size() != orderedGameIds.size()) {
            throw new InvalidFavoritesException("Duplicate gameIds are not allowed");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Load all referenced games and verify every requested ID exists.
        List<VideoGame> games = videoGameRepository.findAllById(orderedGameIds);
        if (games.size() != orderedGameIds.size()) {
            throw new InvalidFavoritesException("One or more gameIds do not exist");
        }
        Map<UUID, VideoGame> gamesById = games.stream()
                .collect(java.util.stream.Collectors.toMap(VideoGame::getId, Function.identity()));

        // Replace: clear then re-insert with 0-based displayOrder.
        // Flush so the delete reaches the DB before re-inserts hit the unique constraint.
        favoriteRepository.deleteByUser(user);
        favoriteRepository.flush();

        for (int i = 0; i < orderedGameIds.size(); i++) {
            VideoGame game = gamesById.get(orderedGameIds.get(i));
            favoriteRepository.save(new Favorite(user, game, i));
        }

        if (!orderedGameIds.isEmpty()) {
            onboardingService.markStepDone(email, OnboardingSteps.FAVORITES);
        }

        return favoriteRepository.findByUserOrderByDisplayOrderAsc(user).stream()
                .sorted(Comparator.comparing(Favorite::getDisplayOrder))
                .map(profileMapper::toFavoriteDto)
                .toList();
    }
}
