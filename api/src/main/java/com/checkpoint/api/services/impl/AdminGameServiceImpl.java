package com.checkpoint.api.services.impl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.dto.admin.CreateGameRequestDto;
import com.checkpoint.api.dto.admin.ExternalGameDto;
import com.checkpoint.api.dto.admin.ImportJobStatusDto;
import com.checkpoint.api.dto.admin.UpdateGameRequestDto;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.ExternalApiUnavailableException;
import com.checkpoint.api.exceptions.ExternalGameNotFoundException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameReferencedException;
import com.checkpoint.api.exceptions.IgdbApiException;
import com.checkpoint.api.jobs.ImportJobRegistry;
import com.checkpoint.api.jobs.ImportJobRunner;
import com.checkpoint.api.jobs.ImportJobStatus;
import com.checkpoint.api.jobs.ImportType;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.CompanyRepository;
import com.checkpoint.api.repositories.FavoriteRepository;
import com.checkpoint.api.repositories.GameListEntryRepository;
import com.checkpoint.api.repositories.GenreRepository;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.PlatformRepository;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserGameRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.AdminGameService;
import com.checkpoint.api.services.GameImportService;

/**
 * Implementation of {@link AdminGameService}.
 * Provides admin operations for searching and importing games from IGDB,
 * plus manual CRUD on video games.
 */
@Service
public class AdminGameServiceImpl implements AdminGameService {

    private static final Logger log = LoggerFactory.getLogger(AdminGameServiceImpl.class);

    private final IgdbApiClient igdbApiClient;
    private final GameImportService gameImportService;
    private final ImportJobRegistry importJobRegistry;
    private final ImportJobRunner importJobRunner;
    private final VideoGameRepository videoGameRepository;
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;
    private final CompanyRepository companyRepository;
    private final UserGameRepository userGameRepository;
    private final UserGamePlayRepository userGamePlayRepository;
    private final ReviewRepository reviewRepository;
    private final BacklogRepository backlogRepository;
    private final WishRepository wishRepository;
    private final FavoriteRepository favoriteRepository;
    private final RateRepository rateRepository;
    private final LikeRepository likeRepository;
    private final GameListEntryRepository gameListEntryRepository;

    public AdminGameServiceImpl(IgdbApiClient igdbApiClient,
                                GameImportService gameImportService,
                                ImportJobRegistry importJobRegistry,
                                ImportJobRunner importJobRunner,
                                VideoGameRepository videoGameRepository,
                                GenreRepository genreRepository,
                                PlatformRepository platformRepository,
                                CompanyRepository companyRepository,
                                UserGameRepository userGameRepository,
                                UserGamePlayRepository userGamePlayRepository,
                                ReviewRepository reviewRepository,
                                BacklogRepository backlogRepository,
                                WishRepository wishRepository,
                                FavoriteRepository favoriteRepository,
                                RateRepository rateRepository,
                                LikeRepository likeRepository,
                                GameListEntryRepository gameListEntryRepository) {
        this.igdbApiClient = igdbApiClient;
        this.gameImportService = gameImportService;
        this.importJobRegistry = importJobRegistry;
        this.importJobRunner = importJobRunner;
        this.videoGameRepository = videoGameRepository;
        this.genreRepository = genreRepository;
        this.platformRepository = platformRepository;
        this.companyRepository = companyRepository;
        this.userGameRepository = userGameRepository;
        this.userGamePlayRepository = userGamePlayRepository;
        this.reviewRepository = reviewRepository;
        this.backlogRepository = backlogRepository;
        this.wishRepository = wishRepository;
        this.favoriteRepository = favoriteRepository;
        this.rateRepository = rateRepository;
        this.likeRepository = likeRepository;
        this.gameListEntryRepository = gameListEntryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalGameDto> searchExternalGames(String query, int limit) {
        log.info("Searching external games with query: '{}', limit: {}", query, limit);

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        try {
            List<IgdbGameDto> igdbGames = igdbApiClient.searchGames(query, limit);

            return igdbGames.stream()
                    .map(this::mapToExternalGameDto)
                    .toList();

        } catch (IgdbApiException e) {
            log.error("IGDB API error during search: {}", e.getMessage(), e);
            throw new ExternalApiUnavailableException("External game API is currently unavailable", e);
        }
    }

    @Override
    public VideoGame importGameByExternalId(Long externalId) {
        log.info("Importing game with external ID: {}", externalId);

        if (externalId == null || externalId <= 0) {
            throw new IllegalArgumentException("External ID must be a positive number");
        }

        try {
            // Fetch the game from IGDB
            List<IgdbGameDto> games = igdbApiClient.fetchGamesByIds(List.of(externalId));

            if (games.isEmpty()) {
                log.warn("Game not found on IGDB with ID: {}", externalId);
                throw new ExternalGameNotFoundException(externalId);
            }

            // Import the game using existing service
            List<VideoGame> importedGames = gameImportService.importGamesByIds(List.of(externalId));

            if (importedGames.isEmpty()) {
                log.error("Failed to import game with external ID: {}", externalId);
                throw new ExternalApiUnavailableException("Failed to import game from external API");
            }

            VideoGame importedGame = importedGames.get(0);
            log.info("Successfully imported game: {} (ID: {})", importedGame.getTitle(), importedGame.getId());

            return importedGame;

        } catch (IgdbApiException e) {
            log.error("IGDB API error during import: {}", e.getMessage(), e);
            throw new ExternalApiUnavailableException("External game API is currently unavailable", e);
        }
    }

    @Override
    public ImportJobStatusDto startTopRatedImport(int limit, int minRatingCount) {
        log.info("Starting async top-rated import (limit={}, minRatingCount={})", limit, minRatingCount);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be a positive number");
        }
        if (minRatingCount < 0) {
            throw new IllegalArgumentException("Minimum rating count cannot be negative");
        }

        ImportJobStatus job = importJobRegistry.startJob(ImportType.TOP_RATED, limit, minRatingCount);
        importJobRunner.run(job);
        return job.toDto();
    }

    @Override
    public ImportJobStatusDto startRecentImport(int limit) {
        log.info("Starting async recent import (limit={})", limit);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be a positive number");
        }

        ImportJobStatus job = importJobRegistry.startJob(ImportType.RECENT, limit, 0);
        importJobRunner.run(job);
        return job.toDto();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportJobStatusDto> findImportJob(UUID jobId) {
        return importJobRegistry.find(jobId).map(ImportJobStatus::toDto);
    }

    @Override
    @Transactional
    public VideoGame createGame(CreateGameRequestDto request) {
        String trimmedTitle = request.title().trim();
        log.info("Admin manual create game: '{}'", trimmedTitle);

        if (videoGameRepository.existsByTitleIgnoreCase(trimmedTitle)) {
            throw new IllegalArgumentException("A game with this title already exists");
        }

        VideoGame game = new VideoGame();
        applyScalarFields(game, trimmedTitle, request.description(), request.coverUrl(),
                request.artworkUrl(), request.trailerYoutubeId(),
                request.timeToBeatNormally(), request.timeToBeatHastily(),
                request.timeToBeatCompletely(), request.releaseDate());
        game.setGenres(resolveGenres(request.genreIds()));
        game.setPlatforms(resolvePlatforms(request.platformIds()));
        game.setCompanies(resolveCompanies(request.companyIds()));

        VideoGame saved = videoGameRepository.save(game);
        log.info("Created game '{}' with ID {}", saved.getTitle(), saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public VideoGame updateGame(UUID gameId, UpdateGameRequestDto request) {
        log.info("Admin manual update game: {}", gameId);

        VideoGame game = videoGameRepository.findByIdWithRelationships(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        String trimmedTitle = request.title().trim();
        if (videoGameRepository.existsByTitleIgnoreCaseAndIdNot(trimmedTitle, gameId)) {
            throw new IllegalArgumentException("Another game already uses this title");
        }

        applyScalarFields(game, trimmedTitle, request.description(), request.coverUrl(),
                request.artworkUrl(), request.trailerYoutubeId(),
                request.timeToBeatNormally(), request.timeToBeatHastily(),
                request.timeToBeatCompletely(), request.releaseDate());
        game.setGenres(resolveGenres(request.genreIds()));
        game.setPlatforms(resolvePlatforms(request.platformIds()));
        game.setCompanies(resolveCompanies(request.companyIds()));

        VideoGame saved = videoGameRepository.save(game);
        log.info("Updated game '{}' (ID {})", saved.getTitle(), saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteGame(UUID gameId) {
        log.info("Admin manual delete game: {}", gameId);

        VideoGame game = videoGameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        Map<String, Long> blocking = collectBlockingReferences(game);
        if (!blocking.isEmpty()) {
            log.warn("Refusing to delete game {} — blocking references: {}", gameId, blocking);
            throw new GameReferencedException(gameId, blocking);
        }

        videoGameRepository.delete(game);
        log.info("Deleted game {} ('{}')", gameId, game.getTitle());
    }

    private Map<String, Long> collectBlockingReferences(VideoGame game) {
        UUID id = game.getId();
        Map<String, Long> blocking = new LinkedHashMap<>();
        putIfPositive(blocking, "library", userGameRepository.countByVideoGameId(id));
        putIfPositive(blocking, "playLogs", userGamePlayRepository.countByVideoGameId(id));
        putIfPositive(blocking, "reviews", reviewRepository.countByVideoGameId(id));
        putIfPositive(blocking, "backlogs", backlogRepository.countByVideoGameId(id));
        putIfPositive(blocking, "wishlists", wishRepository.countByVideoGameId(id));
        putIfPositive(blocking, "favorites", favoriteRepository.countByVideoGameId(id));
        putIfPositive(blocking, "ratings", rateRepository.countByVideoGameId(id));
        putIfPositive(blocking, "likes", likeRepository.countByVideoGameId(id));
        putIfPositive(blocking, "listEntries", gameListEntryRepository.countByVideoGameId(id));
        int dlcCount = game.getDlcs() == null ? 0 : game.getDlcs().size();
        if (dlcCount > 0) {
            blocking.put("dlcs", (long) dlcCount);
        }
        return blocking;
    }

    private static void putIfPositive(Map<String, Long> map, String key, long value) {
        if (value > 0) {
            map.put(key, value);
        }
    }

    private static void applyScalarFields(VideoGame game,
                                          String title,
                                          String description,
                                          String coverUrl,
                                          String artworkUrl,
                                          String trailerYoutubeId,
                                          Long timeToBeatNormally,
                                          Long timeToBeatHastily,
                                          Long timeToBeatCompletely,
                                          java.time.LocalDate releaseDate) {
        game.setTitle(title);
        game.setDescription(blankToNull(description));
        game.setCoverUrl(blankToNull(coverUrl));
        game.setArtworkUrl(blankToNull(artworkUrl));
        game.setTrailerYoutubeId(blankToNull(trailerYoutubeId));
        game.setTimeToBeatNormally(timeToBeatNormally);
        game.setTimeToBeatHastily(timeToBeatHastily);
        game.setTimeToBeatCompletely(timeToBeatCompletely);
        game.setReleaseDate(releaseDate);
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Set<Genre> resolveGenres(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Genre> found = genreRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new IllegalArgumentException("One or more genre IDs are unknown");
        }
        return new HashSet<>(found);
    }

    private Set<Platform> resolvePlatforms(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Platform> found = platformRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new IllegalArgumentException("One or more platform IDs are unknown");
        }
        return new HashSet<>(found);
    }

    private Set<Company> resolveCompanies(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Company> found = companyRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new IllegalArgumentException("One or more company IDs are unknown");
        }
        return new HashSet<>(found);
    }

    /**
     * Maps an IGDB game DTO to a lightweight ExternalGameDto.
     */
    private ExternalGameDto mapToExternalGameDto(IgdbGameDto igdbGame) {
        String coverUrl = null;
        if (igdbGame.cover() != null) {
            coverUrl = igdbGame.cover().getCoverBigUrl();
        }

        return ExternalGameDto.fromIgdb(
                igdbGame.id(),
                igdbGame.name(),
                igdbGame.firstReleaseDate(),
                coverUrl
        );
    }
}
