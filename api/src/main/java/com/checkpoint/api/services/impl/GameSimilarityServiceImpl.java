package com.checkpoint.api.services.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.GameCardDto;
import com.checkpoint.api.entities.Company;
import com.checkpoint.api.entities.Genre;
import com.checkpoint.api.entities.Platform;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.GameSimilarityService;

/**
 * SQL-based item-to-item implementation of {@link GameSimilarityService}.
 *
 * <p>Seeds {@link GameTagScorer} from a single game's genres / platforms / companies,
 * pre-filters the catalog to games sharing at least one genre or company, scores the
 * pool by tag overlap (plus the rating tiebreaker and recency boost), and returns the
 * top matches.</p>
 */
@Service
@Transactional(readOnly = true)
public class GameSimilarityServiceImpl implements GameSimilarityService {

    private static final Logger log = LoggerFactory.getLogger(GameSimilarityServiceImpl.class);

    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 30;
    private static final int CANDIDATE_POOL_CAP = 50;

    private static final double SEED_TAG_WEIGHT = 1.0;

    private final VideoGameRepository videoGameRepository;

    public GameSimilarityServiceImpl(VideoGameRepository videoGameRepository) {
        this.videoGameRepository = videoGameRepository;
    }

    @Override
    public List<GameCardDto> getSimilarGames(UUID gameId, int size) {
        int validatedSize = clampSize(size);

        VideoGame seed = videoGameRepository.findByIdWithRelationships(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        Map<UUID, Double> genreScores = presenceScores(seed.getGenres().stream().map(Genre::getId).toList());
        Map<UUID, Double> platformScores = presenceScores(seed.getPlatforms().stream().map(Platform::getId).toList());
        Map<UUID, Double> companyScores = presenceScores(seed.getCompanies().stream().map(Company::getId).toList());

        if (genreScores.isEmpty() && companyScores.isEmpty()) {
            log.debug("Game {} has no genre/company tags — no similar games", gameId);
            return List.of();
        }

        List<UUID> candidateIds = videoGameRepository.findSimilarCandidateIds(
                gameId,
                GameTagScorer.ensureNonEmpty(genreScores.keySet()),
                GameTagScorer.ensureNonEmpty(companyScores.keySet()),
                PageRequest.of(0, CANDIDATE_POOL_CAP));

        if (candidateIds.isEmpty()) {
            log.debug("No similar candidates for game {}", gameId);
            return List.of();
        }

        List<VideoGame> candidates = videoGameRepository.findAllByIdInWithRelationships(candidateIds);
        LocalDate today = LocalDate.now();

        List<ScoredCandidate> scored = new ArrayList<>(candidates.size());
        for (VideoGame candidate : candidates) {
            GameTagScorer.TagScore tagScore =
                    GameTagScorer.score(candidate, genreScores, platformScores, companyScores, today);
            if (tagScore.total() <= 0) {
                continue;
            }
            scored.add(new ScoredCandidate(candidate.getId(), candidate.getTitle(), tagScore.total()));
        }

        scored.sort(Comparator
                .comparingDouble(ScoredCandidate::score).reversed()
                .thenComparing(ScoredCandidate::title, Comparator.nullsLast(String::compareTo)));

        List<UUID> topIds = new ArrayList<>(validatedSize);
        for (ScoredCandidate s : scored) {
            if (topIds.size() >= validatedSize) {
                break;
            }
            topIds.add(s.id());
        }

        Map<UUID, GameCardDto> cardsById = videoGameRepository.findGameCardsByIdIn(topIds).stream()
                .collect(Collectors.toMap(GameCardDto::id, Function.identity()));

        List<GameCardDto> result = new ArrayList<>(topIds.size());
        for (UUID id : topIds) {
            GameCardDto card = cardsById.get(id);
            if (card != null) {
                result.add(card);
            }
        }
        return result;
    }

    private static Map<UUID, Double> presenceScores(List<UUID> ids) {
        Map<UUID, Double> scores = new HashMap<>();
        for (UUID id : ids) {
            scores.put(id, SEED_TAG_WEIGHT);
        }
        return scores;
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private record ScoredCandidate(UUID id, String title, double score) {
    }
}
