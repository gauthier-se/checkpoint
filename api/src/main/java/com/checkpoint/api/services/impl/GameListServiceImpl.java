package com.checkpoint.api.services.impl;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.list.AddGameToListRequestDto;
import com.checkpoint.api.dto.list.CreateGameListRequestDto;
import com.checkpoint.api.dto.list.GameListCardDto;
import com.checkpoint.api.dto.list.GameListDetailDto;
import com.checkpoint.api.dto.list.ReorderGamesRequestDto;
import com.checkpoint.api.dto.list.UpdateGameListRequestDto;
import com.checkpoint.api.entities.GameList;
import com.checkpoint.api.entities.GameListEntry;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.events.ListCreatedEvent;
import com.checkpoint.api.events.UserActivityEvent;
import com.checkpoint.api.exceptions.GameAlreadyInListException;
import com.checkpoint.api.exceptions.GameListNotFoundException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameNotInListException;
import com.checkpoint.api.exceptions.UnauthorizedListAccessException;
import com.checkpoint.api.mapper.GameListMapper;
import com.checkpoint.api.repositories.CommentRepository;
import com.checkpoint.api.repositories.GameListEntryRepository;
import com.checkpoint.api.repositories.GameListRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.GameListService;

/**
 * Implementation of {@link GameListService}.
 */
@Service
@Transactional
public class GameListServiceImpl implements GameListService {

    private static final Logger log = LoggerFactory.getLogger(GameListServiceImpl.class);

    private final GameListRepository gameListRepository;
    private final GameListEntryRepository gameListEntryRepository;
    private final UserRepository userRepository;
    private final VideoGameRepository videoGameRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final GameListMapper gameListMapper;
    private final ApplicationEventPublisher eventPublisher;

    public GameListServiceImpl(GameListRepository gameListRepository,
                               GameListEntryRepository gameListEntryRepository,
                               UserRepository userRepository,
                               VideoGameRepository videoGameRepository,
                               LikeRepository likeRepository,
                               CommentRepository commentRepository,
                               GameListMapper gameListMapper,
                               ApplicationEventPublisher eventPublisher) {
        this.gameListRepository = gameListRepository;
        this.gameListEntryRepository = gameListEntryRepository;
        this.userRepository = userRepository;
        this.videoGameRepository = videoGameRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.gameListMapper = gameListMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public GameListDetailDto createList(String userEmail, CreateGameListRequestDto request) {
        log.debug("Creating list '{}' for user {}", request.title(), userEmail);

        User user = findUserByEmail(userEmail);

        GameList gameList = new GameList(request.title(), user);
        gameList.setDescription(request.description());
        gameList.setIsPrivate(request.isPrivate() != null ? request.isPrivate() : false);

        GameList saved = gameListRepository.save(gameList);

        // First-list XP: only on the user's very first list AND only if public.
        // Counted after the save, so the threshold is exactly 1.
        if (!saved.getIsPrivate() && gameListRepository.countByUserId(user.getId()) == 1) {
            eventPublisher.publishEvent(new ListCreatedEvent(user.getId(), saved.getId()));
        }
        eventPublisher.publishEvent(new UserActivityEvent(user.getId()));

        log.info("List '{}' created with ID {} for user {}", saved.getTitle(), saved.getId(), userEmail);
        return gameListMapper.toDetailDto(saved, List.of(), 0L, 0L, true, false);
    }

    @Override
    public GameListDetailDto updateList(String userEmail, UUID listId, UpdateGameListRequestDto request) {
        log.debug("Updating list {} for user {}", listId, userEmail);

        User user = findUserByEmail(userEmail);
        GameList gameList = findGameListById(listId);
        enforceOwnership(gameList, user);

        if (request.title() != null) {
            gameList.setTitle(request.title());
        }
        if (request.description() != null) {
            gameList.setDescription(request.description());
        }
        if (request.isPrivate() != null) {
            gameList.setIsPrivate(request.isPrivate());
        }

        GameList updated = gameListRepository.save(gameList);
        List<GameListEntry> entries = gameListEntryRepository.findByGameListIdOrderByPositionAsc(listId);
        long likesCount = likeRepository.countByGameListId(listId);

        long commentsCount = commentRepository.countByGameListId(listId);

        eventPublisher.publishEvent(new UserActivityEvent(user.getId()));

        log.info("List '{}' updated for user {}", updated.getTitle(), userEmail);
        return gameListMapper.toDetailDto(updated, entries, likesCount, commentsCount, true, false);
    }

    @Override
    public void deleteList(String userEmail, UUID listId) {
        log.debug("Deleting list {} for user {}", listId, userEmail);

        User user = findUserByEmail(userEmail);
        GameList gameList = findGameListById(listId);
        enforceOwnership(gameList, user);

        gameListRepository.delete(gameList);
        log.info("List {} deleted for user {}", listId, userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GameListCardDto> getUserLists(String userEmail, Pageable pageable) {
        log.debug("Fetching lists for user {} - page: {}, size: {}",
                userEmail, pageable.getPageNumber(), pageable.getPageSize());

        User user = findUserByEmail(userEmail);
        return gameListRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toCardDtoWithLikes);
    }

    @Override
    public GameListDetailDto addGameToList(String userEmail, UUID listId, AddGameToListRequestDto request) {
        log.debug("Adding game {} to list {} for user {}", request.videoGameId(), listId, userEmail);

        User user = findUserByEmail(userEmail);
        GameList gameList = findGameListById(listId);
        enforceOwnership(gameList, user);

        VideoGame videoGame = videoGameRepository.findById(request.videoGameId())
                .orElseThrow(() -> new GameNotFoundException(request.videoGameId()));

        if (gameListEntryRepository.existsByGameListIdAndVideoGameId(listId, videoGame.getId())) {
            throw new GameAlreadyInListException(videoGame.getId());
        }

        Integer maxPosition = gameListEntryRepository.findMaxPositionByGameListId(listId).orElse(-1);
        GameListEntry entry = new GameListEntry(gameList, videoGame, maxPosition + 1);
        gameListEntryRepository.save(entry);

        List<GameListEntry> entries = gameListEntryRepository.findByGameListIdOrderByPositionAsc(listId);
        long likesCount = likeRepository.countByGameListId(listId);

        long commentsCount = commentRepository.countByGameListId(listId);

        eventPublisher.publishEvent(new UserActivityEvent(user.getId()));

        log.info("Game '{}' added to list '{}' at position {}", videoGame.getTitle(), gameList.getTitle(), entry.getPosition());
        return gameListMapper.toDetailDto(gameList, entries, likesCount, commentsCount, true, false);
    }

    @Override
    public void removeGameFromList(String userEmail, UUID listId, UUID videoGameId) {
        log.debug("Removing game {} from list {} for user {}", videoGameId, listId, userEmail);

        User user = findUserByEmail(userEmail);
        GameList gameList = findGameListById(listId);
        enforceOwnership(gameList, user);

        if (!gameListEntryRepository.existsByGameListIdAndVideoGameId(listId, videoGameId)) {
            throw new GameNotInListException(videoGameId);
        }

        gameListEntryRepository.deleteByGameListIdAndVideoGameId(listId, videoGameId);

        // Re-index positions after removal
        List<GameListEntry> remaining = gameListEntryRepository.findByGameListIdOrderByPositionAsc(listId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        gameListEntryRepository.saveAll(remaining);

        eventPublisher.publishEvent(new UserActivityEvent(user.getId()));

        log.info("Game {} removed from list {}", videoGameId, listId);
    }

    @Override
    public GameListDetailDto reorderGames(String userEmail, UUID listId, ReorderGamesRequestDto request) {
        log.debug("Reordering games in list {} for user {}", listId, userEmail);

        User user = findUserByEmail(userEmail);
        GameList gameList = findGameListById(listId);
        enforceOwnership(gameList, user);

        List<UUID> orderedIds = request.orderedVideoGameIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID videoGameId = orderedIds.get(i);
            GameListEntry entry = gameListEntryRepository
                    .findByGameListIdAndVideoGameId(listId, videoGameId)
                    .orElseThrow(() -> new GameNotInListException(videoGameId));
            entry.setPosition(i);
            gameListEntryRepository.save(entry);
        }

        List<GameListEntry> entries = gameListEntryRepository.findByGameListIdOrderByPositionAsc(listId);
        long likesCount = likeRepository.countByGameListId(listId);

        long commentsCount = commentRepository.countByGameListId(listId);

        eventPublisher.publishEvent(new UserActivityEvent(user.getId()));

        log.info("Games reordered in list '{}'", gameList.getTitle());
        return gameListMapper.toDetailDto(gameList, entries, likesCount, commentsCount, true, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GameListCardDto> getRecentPublicLists(Pageable pageable) {
        log.debug("Fetching recent public lists - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return gameListRepository.findAllPublic(pageable)
                .map(this::toCardDtoWithLikes);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GameListCardDto> getPopularPublicLists(Pageable pageable) {
        log.debug("Fetching popular public lists - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return gameListRepository.findPopularPublic(pageable)
                .map(this::toCardDtoWithLikes);
    }

    @Override
    @Transactional(readOnly = true)
    public GameListDetailDto getListDetail(UUID listId, String viewerEmail) {
        log.debug("Fetching list detail {} - viewer: {}", listId,
                viewerEmail != null ? viewerEmail : "anonymous");

        GameList gameList = findGameListById(listId);

        boolean isOwner = false;
        boolean hasLiked = false;

        if (viewerEmail != null) {
            User viewer = userRepository.findByEmail(viewerEmail).orElse(null);
            if (viewer != null) {
                isOwner = gameList.getUser().getId().equals(viewer.getId());
                hasLiked = likeRepository.existsByUserIdAndGameListId(viewer.getId(), listId);
            }
        }

        // Private lists are only accessible to the owner
        if (gameList.getIsPrivate() && !isOwner) {
            throw new UnauthorizedListAccessException(listId);
        }

        List<GameListEntry> entries = gameListEntryRepository.findByGameListIdOrderByPositionAsc(listId);
        long likesCount = likeRepository.countByGameListId(listId);
        long commentsCount = commentRepository.countByGameListId(listId);

        return gameListMapper.toDetailDto(gameList, entries, likesCount, commentsCount, isOwner, hasLiked);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GameListCardDto> getUserPublicLists(String username, Pageable pageable) {
        log.debug("Fetching public lists for user {} - page: {}, size: {}",
                username, pageable.getPageNumber(), pageable.getPageSize());

        return gameListRepository.findPublicByUserPseudo(username, pageable)
                .map(this::toCardDtoWithLikes);
    }

    // ---- Helper methods ----

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    private GameList findGameListById(UUID listId) {
        return gameListRepository.findById(listId)
                .orElseThrow(() -> new GameListNotFoundException(listId));
    }

    private void enforceOwnership(GameList gameList, User user) {
        if (!gameList.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedListAccessException(gameList.getId());
        }
    }

    private GameListCardDto toCardDtoWithLikes(GameList gameList) {
        long likesCount = likeRepository.countByGameListId(gameList.getId());
        long commentsCount = commentRepository.countByGameListId(gameList.getId());
        List<GameListEntry> entries = gameListEntryRepository.findByGameListIdOrderByPositionAsc(gameList.getId());
        List<String> coverUrls = entries.stream()
                .limit(4)
                .map(entry -> entry.getVideoGame().getCoverUrl())
                .toList();
        return gameListMapper.toCardDto(gameList, likesCount, commentsCount, coverUrls);
    }
}
