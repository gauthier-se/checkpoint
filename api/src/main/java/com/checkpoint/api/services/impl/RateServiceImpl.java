package com.checkpoint.api.services.impl;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.RateResponseDto;
import com.checkpoint.api.entities.Rate;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.VideoGame;
import com.checkpoint.api.events.GameRatedEvent;
import com.checkpoint.api.events.UserActivityEvent;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.RateNotFoundException;
import com.checkpoint.api.mapper.RateMapper;
import com.checkpoint.api.repositories.RateRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.VideoGameRepository;
import com.checkpoint.api.services.RateService;

/**
 * Implementation of {@link RateService}.
 * Manages standalone game ratings independently from reviews.
 */
@Service
@Transactional
public class RateServiceImpl implements RateService {

    private static final Logger log = LoggerFactory.getLogger(RateServiceImpl.class);

    private final RateRepository rateRepository;
    private final VideoGameRepository videoGameRepository;
    private final UserRepository userRepository;
    private final RateMapper rateMapper;
    private final ApplicationEventPublisher eventPublisher;

    public RateServiceImpl(RateRepository rateRepository,
                           VideoGameRepository videoGameRepository,
                           UserRepository userRepository,
                           RateMapper rateMapper,
                           ApplicationEventPublisher eventPublisher) {
        this.rateRepository = rateRepository;
        this.videoGameRepository = videoGameRepository;
        this.userRepository = userRepository;
        this.rateMapper = rateMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RateResponseDto rateGame(String userEmail, UUID videoGameId, Integer score) {
        log.info("Rating game {} with score {} for user {}", videoGameId, score, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));

        VideoGame videoGame = videoGameRepository.findById(videoGameId)
                .orElseThrow(() -> new GameNotFoundException(videoGameId));

        Optional<Rate> existing = rateRepository.findByUserEmailAndVideoGameId(userEmail, videoGameId);
        boolean firstTime = existing.isEmpty();

        Rate rate = existing.orElseGet(() -> new Rate(user, videoGame, score));
        rate.setScore(score);
        Rate savedRate = rateRepository.save(rate);

        updateGameAverageRating(videoGame);

        if (firstTime) {
            eventPublisher.publishEvent(new GameRatedEvent(user.getId(), videoGameId));
        }
        eventPublisher.publishEvent(new UserActivityEvent(user.getId()));

        return rateMapper.toDto(savedRate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRating(String userEmail, UUID videoGameId) {
        log.info("Removing rating for game {} for user {}", videoGameId, userEmail);

        Rate rate = rateRepository.findByUserEmailAndVideoGameId(userEmail, videoGameId)
                .orElseThrow(() -> new RateNotFoundException(videoGameId));

        VideoGame videoGame = rate.getVideoGame();
        rateRepository.delete(rate);

        updateGameAverageRating(videoGame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public RateResponseDto getUserRating(String userEmail, UUID videoGameId) {
        log.debug("Fetching rating for game {} for user {}", videoGameId, userEmail);

        Rate rate = rateRepository.findByUserEmailAndVideoGameId(userEmail, videoGameId)
                .orElse(null);

        return rateMapper.toDto(rate);
    }

    /**
     * Recalculates and persists the average rating for a video game.
     *
     * <p>Raw scores are stored as 1-10 (half-star steps). The average is divided
     * by 2 here so {@code averageRating} stays in the 0-5 display range with
     * 0.1 precision — no conversion needed on the frontend.</p>
     *
     * @param videoGame the video game entity to update
     */
    private void updateGameAverageRating(VideoGame videoGame) {
        rateRepository.flush();
        Double avg = rateRepository.calculateAverageRating(videoGame.getId());
        double averageRating = (avg != null) ? avg / 2.0 : 0.0;

        averageRating = Math.round(averageRating * 10.0) / 10.0;

        videoGame.setAverageRating(averageRating);
        videoGameRepository.save(videoGame);
    }
}
