package com.checkpoint.api.services.impl;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.igdb.IgdbCompanyDto;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbGenreDto;
import com.checkpoint.api.dto.igdb.IgdbInvolvedCompanyDto;
import com.checkpoint.api.dto.igdb.IgdbPlatformDto;
import com.checkpoint.api.dto.igdb.IgdbTimeToBeatDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.mapper.GameMapper;
import com.checkpoint.api.repositories.CompanyRepository;
import com.checkpoint.api.repositories.GenreRepository;
import com.checkpoint.api.repositories.PlatformRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.GamePersistenceService;

/**
 * Implementation of {@link GamePersistenceService}.
 * Each call to {@link #importOne(IgdbGameDto, IgdbTimeToBeatDto)} runs in its own
 * new transaction ({@link Propagation#REQUIRES_NEW}).
 */
@Service
public class GamePersistenceServiceImpl implements GamePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(GamePersistenceServiceImpl.class);

    private final GameMapper gameMapper;
    private final VideoGameRepository videoGameRepository;
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;
    private final CompanyRepository companyRepository;

    public GamePersistenceServiceImpl(
            GameMapper gameMapper,
            VideoGameRepository videoGameRepository,
            GenreRepository genreRepository,
            PlatformRepository platformRepository,
            CompanyRepository companyRepository) {
        this.gameMapper = gameMapper;
        this.videoGameRepository = videoGameRepository;
        this.genreRepository = genreRepository;
        this.platformRepository = platformRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VideoGame importOne(IgdbGameDto dto, IgdbTimeToBeatDto timeToBeat) {
        // Check for existing game by IGDB ID (duplicate handling / upsert)
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

        // Save first to generate UUID (required by Hibernate Search indexing)
        videoGame = videoGameRepository.save(videoGame);

        // Resolve and set relationships (now that the entity has an ID)
        resolveAndSetGenres(dto, videoGame);
        resolveAndSetPlatforms(dto, videoGame);
        resolveAndSetCompanies(dto, videoGame);
        applyTimeToBeat(videoGame, timeToBeat);

        return videoGameRepository.save(videoGame);
    }

    /**
     * Applies the pre-fetched time-to-beat statistics onto the entity, if present.
     */
    private void applyTimeToBeat(VideoGame videoGame, IgdbTimeToBeatDto ttb) {
        if (ttb == null) {
            return;
        }
        videoGame.setTimeToBeatNormally(ttb.normally());
        videoGame.setTimeToBeatHastily(ttb.hastily());
        videoGame.setTimeToBeatCompletely(ttb.completely());
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
            genres.add(findOrCreateGenre(genreDto.name()));
        }

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
            platforms.add(findOrCreatePlatform(platformDto.name()));
        }

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
            if (involvement.developer() || involvement.publisher()) {
                IgdbCompanyDto companyDto = involvement.company();
                if (companyDto != null && companyDto.name() != null) {
                    companies.add(findOrCreateCompany(companyDto.name(), companyDto.description()));
                }
            }
        }

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
