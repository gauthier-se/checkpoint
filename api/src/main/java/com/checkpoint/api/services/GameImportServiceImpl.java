package com.checkpoint.api.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.client.IgdbApiClient;
import com.checkpoint.api.dto.igdb.IgdbCompanyDto;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbGenreDto;
import com.checkpoint.api.dto.igdb.IgdbInvolvedCompanyDto;
import com.checkpoint.api.dto.igdb.IgdbPlatformDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.mapper.GameMapper;
import com.checkpoint.api.repositories.CompanyRepository;
import com.checkpoint.api.repositories.GenreRepository;
import com.checkpoint.api.repositories.PlatformRepository;
import com.checkpoint.api.repositories.VideoGameRepository;

/**
 * Implementation of {@link GameImportService}.
 * Handles the orchestration of game imports from IGDB with:
 * - Duplicate detection and update (upsert behavior)
 * - Relationship management (genres, platforms, companies)
 * - Rate limiting is handled by the IgdbApiClient
 */
@Service
@Transactional
public class GameImportServiceImpl implements GameImportService {

    private static final Logger log = LoggerFactory.getLogger(GameImportServiceImpl.class);

    private final IgdbApiClient igdbApiClient;
    private final GameMapper gameMapper;
    private final VideoGameRepository videoGameRepository;
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;
    private final CompanyRepository companyRepository;

    public GameImportServiceImpl(
            IgdbApiClient igdbApiClient,
            GameMapper gameMapper,
            VideoGameRepository videoGameRepository,
            GenreRepository genreRepository,
            PlatformRepository platformRepository,
            CompanyRepository companyRepository) {
        this.igdbApiClient = igdbApiClient;
        this.gameMapper = gameMapper;
        this.videoGameRepository = videoGameRepository;
        this.genreRepository = genreRepository;
        this.platformRepository = platformRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<VideoGame> importRecentlyReleasedGames(int limit) {
        log.info("Importing {} recently released games", limit);
        List<IgdbGameDto> games = igdbApiClient.fetchRecentlyReleasedGames(limit);
        return importGames(games);
    }

    @Override
    public List<VideoGame> importGamesByIds(List<Long> igdbIds) {
        log.info("Importing {} games by IDs", igdbIds.size());
        List<IgdbGameDto> games = igdbApiClient.fetchGamesByIds(igdbIds);
        return importGames(games);
    }

    @Override
    public List<VideoGame> searchAndImportGames(String query, int limit) {
        log.info("Searching and importing games matching '{}'", query);
        List<IgdbGameDto> games = igdbApiClient.searchGames(query, limit);
        return importGames(games);
    }

    @Override
    public List<VideoGame> importTopRatedGames(int limit, int minRatingCount) {
        log.info("Importing top {} rated games (min {} ratings)", limit, minRatingCount);
        List<IgdbGameDto> games = igdbApiClient.fetchTopRatedGames(limit, minRatingCount);
        return importGames(games);
    }

    /**
     * Imports a list of games, handling duplicates and relationships.
     *
     * @param games list of IGDB game DTOs to import
     * @return list of persisted VideoGame entities
     */
    private List<VideoGame> importGames(List<IgdbGameDto> games) {
        List<VideoGame> result = new ArrayList<>();
        int created = 0;
        int updated = 0;

        for (IgdbGameDto dto : games) {
            try {
                VideoGame videoGame = importSingleGame(dto);
                result.add(videoGame);

                if (videoGame.getId() == null) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                log.error("Failed to import game '{}' (IGDB ID: {}): {}",
                        dto.name(), dto.id(), e.getMessage(), e);
            }
        }

        log.info("Import completed: {} created, {} updated, {} failed out of {} total",
                created, updated, games.size() - created - updated, games.size());

        return result;
    }

    /**
     * Imports or updates a single game with all its relationships.
     *
     * @param dto the IGDB game DTO
     * @return the persisted VideoGame entity
     */
    private VideoGame importSingleGame(IgdbGameDto dto) {
        // Check for existing game by IGDB ID (duplicate handling)
        Optional<VideoGame> existingGame = videoGameRepository.findByIgdbId(dto.id());

        VideoGame videoGame;
        if (existingGame.isPresent()) {
            log.debug("Updating existing game: {} (IGDB ID: {})", dto.name(), dto.id());
            videoGame = existingGame.get();
            gameMapper.updateEntity(dto, videoGame);
        } else {
            log.debug("Creating new game: {} (IGDB ID: {})", dto.name(), dto.id());
            videoGame = gameMapper.toEntity(dto);
        }

        // Resolve and set relationships
        resolveAndSetGenres(dto, videoGame);
        resolveAndSetPlatforms(dto, videoGame);
        resolveAndSetCompanies(dto, videoGame);

        return videoGameRepository.save(videoGame);
    }

    /**
     * Resolves genre entities (creating them if necessary) and associates them with the game.
     */
    private void resolveAndSetGenres(IgdbGameDto dto, VideoGame videoGame) {
        if (dto.genres() == null || dto.genres().isEmpty()) {
            return;
        }

        Set<Genre> genres = new HashSet<>();
        for (IgdbGenreDto genreDto : dto.genres()) {
            Genre genre = findOrCreateGenre(genreDto.name());
            genres.add(genre);
        }

        // Clear existing genres and add new ones
        videoGame.getGenres().clear();
        for (Genre genre : genres) {
            videoGame.addGenre(genre);
        }
    }

    /**
     * Resolves platform entities (creating them if necessary) and associates them with the game.
     */
    private void resolveAndSetPlatforms(IgdbGameDto dto, VideoGame videoGame) {
        if (dto.platforms() == null || dto.platforms().isEmpty()) {
            return;
        }

        Set<Platform> platforms = new HashSet<>();
        for (IgdbPlatformDto platformDto : dto.platforms()) {
            Platform platform = findOrCreatePlatform(platformDto.name());
            platforms.add(platform);
        }

        // Clear existing platforms and add new ones
        videoGame.getPlatforms().clear();
        for (Platform platform : platforms) {
            videoGame.addPlatform(platform);
        }
    }

    /**
     * Resolves company entities (creating them if necessary) and associates them with the game.
     * Only includes developers and publishers.
     */
    private void resolveAndSetCompanies(IgdbGameDto dto, VideoGame videoGame) {
        if (dto.involvedCompanies() == null || dto.involvedCompanies().isEmpty()) {
            return;
        }

        Set<Company> companies = new HashSet<>();
        for (IgdbInvolvedCompanyDto involvement : dto.involvedCompanies()) {
            // Only include developers and publishers
            if (involvement.developer() || involvement.publisher()) {
                IgdbCompanyDto companyDto = involvement.company();
                if (companyDto != null && companyDto.name() != null) {
                    Company company = findOrCreateCompany(companyDto.name(), companyDto.description());
                    companies.add(company);
                }
            }
        }

        // Clear existing companies and add new ones
        videoGame.getCompanies().clear();
        for (Company company : companies) {
            videoGame.addCompany(company);
        }
    }

    /**
     * Finds an existing genre by name or creates a new one.
     */
    private Genre findOrCreateGenre(String name) {
        return genreRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    log.debug("Creating new genre: {}", name);
                    return genreRepository.save(new Genre(name));
                });
    }

    /**
     * Finds an existing platform by name or creates a new one.
     */
    private Platform findOrCreatePlatform(String name) {
        return platformRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    log.debug("Creating new platform: {}", name);
                    return platformRepository.save(new Platform(name));
                });
    }

    /**
     * Finds an existing company by name or creates a new one.
     */
    private Company findOrCreateCompany(String name, String description) {
        return companyRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    log.debug("Creating new company: {}", name);
                    return companyRepository.save(new Company(name, description));
                });
    }
}
