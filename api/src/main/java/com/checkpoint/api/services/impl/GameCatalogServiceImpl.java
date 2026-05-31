package com.checkpoint.api.services.impl;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.dto.catalog.GameDetailDto;
import com.checkpoint.api.dto.catalog.GameDetailDto.CompanyDto;
import com.checkpoint.api.dto.catalog.GameDetailDto.GenreDto;
import com.checkpoint.api.dto.catalog.GameDetailDto.PlatformDto;
import com.checkpoint.api.dto.profile.RatingDistributionEntryDto;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.repositories.BacklogRepository;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.GameCatalogService;

/**
 * Implementation of {@link GameCatalogService}.
 * Provides optimized queries for game catalog operations.
 */
@Service
@Transactional(readOnly = true)
public class GameCatalogServiceImpl implements GameCatalogService {

    private static final Logger log = LoggerFactory.getLogger(GameCatalogServiceImpl.class);

    private final VideoGameRepository videoGameRepository;
    private final BacklogRepository backlogRepository;
    private final WishRepository wishRepository;
    private final RateRepository rateRepository;

    public GameCatalogServiceImpl(VideoGameRepository videoGameRepository,
                                  BacklogRepository backlogRepository,
                                  WishRepository wishRepository,
                                  RateRepository rateRepository) {
        this.videoGameRepository = videoGameRepository;
        this.backlogRepository = backlogRepository;
        this.wishRepository = wishRepository;
        this.rateRepository = rateRepository;
    }

    @Override
    public Page<GameCardDto> getGameCatalog(Pageable pageable,
                                             List<String> genres,
                                             List<String> platforms,
                                             Integer yearMin,
                                             Integer yearMax,
                                             Double ratingMin,
                                             Double ratingMax) {
        log.debug("Fetching game catalog - page: {}, size: {}, sort: {}, genres: {}, platforms: {}, "
                + "yearMin: {}, yearMax: {}, ratingMin: {}, ratingMax: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort(),
                genres, platforms, yearMin, yearMax, ratingMin, ratingMax);

        return videoGameRepository.findAllAsGameCardsWithFilters(
                pageable, genres, platforms, yearMin, yearMax, ratingMin, ratingMax);
    }

    @Override
    public List<GameCardDto> getMostBackloggedGames(int size) {
        log.debug("Fetching most-backlogged games - size: {}", size);
        return backlogRepository.findMostBackloggedGames(PageRequest.of(0, size)).getContent();
    }

    @Override
    public List<GameCardDto> getMostWishlistedGames(int size) {
        log.debug("Fetching most-wishlisted games - size: {}", size);
        return wishRepository.findMostWishlistedGames(PageRequest.of(0, size)).getContent();
    }

    @Override
    public GameDetailDto getGameDetails(UUID id) {
        log.debug("Fetching game details for ID: {}", id);

        VideoGame game = videoGameRepository.findByIdWithRelationships(id)
                .orElseThrow(() -> new GameNotFoundException(id));

        // Get rating statistics
        Double averageRating = game.getAverageRating();
        Long ratingCount = videoGameRepository.countRatings(id);
        List<RatingDistributionEntryDto> ratingDistribution =
                rateRepository.findDistributionByVideoGameId(id);

        return mapToGameDetailDto(game, averageRating, ratingCount, ratingDistribution);
    }

    /**
     * Maps a VideoGame entity to a GameDetailDto.
     */
    private GameDetailDto mapToGameDetailDto(VideoGame game, Double averageRating, Long ratingCount,
                                             List<RatingDistributionEntryDto> ratingDistribution) {
        List<GenreDto> genres = game.getGenres().stream()
                .map(g -> new GenreDto(g.getId(), g.getName()))
                .toList();

        List<PlatformDto> platforms = game.getPlatforms().stream()
                .map(p -> new PlatformDto(p.getId(), p.getName()))
                .toList();

        List<CompanyDto> companies = game.getCompanies().stream()
                .map(c -> new CompanyDto(c.getId(), c.getName()))
                .toList();

        // Round average rating to 1 decimal place
        Double roundedRating = averageRating != null
                ? Math.round(averageRating * 10.0) / 10.0
                : null;

        return new GameDetailDto(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getCoverUrl(),
                game.getArtworkUrl(),
                game.getTrailerYoutubeId(),
                game.getTimeToBeatNormally(),
                game.getTimeToBeatHastily(),
                game.getTimeToBeatCompletely(),
                game.getReleaseDate(),
                roundedRating,
                ratingCount != null ? ratingCount : 0L,
                ratingDistribution,
                genres,
                platforms,
                companies
        );
    }
}
