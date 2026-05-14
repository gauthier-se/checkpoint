package com.checkpoint.api.services.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.collection.UserGameRequestDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.UserGame;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.GameAlreadyInLibraryException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameNotInLibraryException;
import com.checkpoint.api.mapper.UserGameMapper;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.UserGameCollectionService;

/**
 * Implementation of {@link UserGameCollectionService}.
 * Manages the user's personal game library (collection).
 */
@Service
@Transactional
public class UserGameCollectionServiceImpl implements UserGameCollectionService {

    private static final Logger log = LoggerFactory.getLogger(UserGameCollectionServiceImpl.class);

    private final UserGameRepository userGameRepository;
    private final UserRepository userRepository;
    private final VideoGameRepository videoGameRepository;
    private final UserGameMapper userGameMapper;

    public UserGameCollectionServiceImpl(UserGameRepository userGameRepository,
                                         UserRepository userRepository,
                                         VideoGameRepository videoGameRepository,
                                         UserGameMapper userGameMapper) {
        this.userGameRepository = userGameRepository;
        this.userRepository = userRepository;
        this.videoGameRepository = videoGameRepository;
        this.userGameMapper = userGameMapper;
    }

    @Override
    public UserGameResponseDto addGameToLibrary(String userEmail, UserGameRequestDto request) {
        log.debug("Adding game {} to library for user {}", request.videoGameId(), userEmail);

        User user = findUserByEmail(userEmail);
        VideoGame videoGame = findVideoGameById(request.videoGameId());

        if (userGameRepository.existsByUserIdAndVideoGameId(user.getId(), videoGame.getId())) {
            throw new GameAlreadyInLibraryException(videoGame.getId());
        }

        UserGame userGame = new UserGame(user, videoGame, request.status());
        userGame.setNotes(request.notes());
        UserGame saved = userGameRepository.save(userGame);

        log.info("Game {} added to library for user {} with status {}", videoGame.getTitle(), userEmail, request.status());
        return userGameMapper.toResponseDto(saved);
    }

    @Override
    public UserGameResponseDto updateGameStatus(String userEmail, UUID videoGameId, UserGameRequestDto request) {
        log.debug("Updating game {} status to {} for user {}", videoGameId, request.status(), userEmail);

        User user = findUserByEmail(userEmail);

        UserGame userGame = userGameRepository.findByUserIdAndVideoGameId(user.getId(), videoGameId)
                .orElseThrow(() -> new GameNotInLibraryException(videoGameId));

        userGame.setStatus(request.status());
        userGame.setNotes(request.notes());
        UserGame updated = userGameRepository.save(userGame);

        log.info("Game {} status updated to {} for user {}", videoGameId, request.status(), userEmail);
        return userGameMapper.toResponseDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserGameResponseDto> getUserLibrary(String userEmail, Pageable pageable) {
        log.debug("Fetching library for user {} - page: {}, size: {}",
                userEmail, pageable.getPageNumber(), pageable.getPageSize());

        User user = findUserByEmail(userEmail);

        return userGameRepository.findByUserIdWithVideoGame(user.getId(), pageable)
                .map(userGameMapper::toResponseDto);
    }

    @Override
    public void removeGameFromLibrary(String userEmail, UUID videoGameId) {
        log.debug("Removing game {} from library for user {}", videoGameId, userEmail);

        User user = findUserByEmail(userEmail);

        if (!userGameRepository.existsByUserIdAndVideoGameId(user.getId(), videoGameId)) {
            throw new GameNotInLibraryException(videoGameId);
        }

        userGameRepository.deleteByUserIdAndVideoGameId(user.getId(), videoGameId);
        log.info("Game {} removed from library for user {}", videoGameId, userEmail);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    private VideoGame findVideoGameById(UUID id) {
        return videoGameRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException(id));
    }
}
