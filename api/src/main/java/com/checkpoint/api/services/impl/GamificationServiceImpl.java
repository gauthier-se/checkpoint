package com.checkpoint.api.services.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.entities.User;
import com.checkpoint.api.entities.XpGrant;
import com.checkpoint.api.enums.XpEventType;
import com.checkpoint.api.events.UserLeveledUpEvent;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.XpGrantRepository;
import com.checkpoint.api.services.GamificationService;

/**
 * Implementation of {@link GamificationService}.
 * Manages XP awards and automatic level progression.
 */
@Service
@Transactional
public class GamificationServiceImpl implements GamificationService {

    private static final Logger log = LoggerFactory.getLogger(GamificationServiceImpl.class);

    private final UserRepository userRepository;
    private final XpGrantRepository xpGrantRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs a new GamificationServiceImpl.
     *
     * @param userRepository    the user repository
     * @param xpGrantRepository the XP grant audit repository
     * @param eventPublisher    Spring's application event publisher, used to broadcast level-ups
     */
    public GamificationServiceImpl(UserRepository userRepository,
                                   XpGrantRepository xpGrantRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.xpGrantRepository = xpGrantRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addXp(UUID userId, int xpAmount) {
        applyXp(userId, xpAmount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void awardXp(UUID userId, int xpAmount, XpEventType eventType, UUID targetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        try {
            xpGrantRepository.saveAndFlush(new XpGrant(user, eventType, targetId, xpAmount));
        } catch (DataIntegrityViolationException ex) {
            // Unique constraint hit (user_id, event_type, target_id) -> already granted, skip.
            log.debug("Skipping duplicate XP grant: user={}, type={}, target={}", userId, eventType, targetId);
            return;
        }

        applyXp(userId, xpAmount);
    }

    /**
     * Applies a flat XP delta to a user and handles level-up. Shared by both
     * the dedup-aware and legacy code paths.
     *
     * @param userId   the user to credit
     * @param xpAmount the XP delta
     */
    private void applyXp(UUID userId, int xpAmount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        Integer currentXp = user.getXpPoint();
        Integer newXp = currentXp + xpAmount;
        user.setXpPoint(newXp);

        Integer currentLevel = user.getLevel();
        Integer newLevel = currentLevel;

        while (newXp >= newLevel * 1000) {
            newLevel++;
        }

        boolean leveledUp = newLevel > currentLevel;
        if (leveledUp) {
            user.setLevel(newLevel);
            log.info("User {} leveled up from {} to {} (XP: {})", userId, currentLevel, newLevel, newXp);
        }

        userRepository.save(user);
        log.info("Awarded {} XP to user {} (total: {})", xpAmount, userId, newXp);

        if (leveledUp) {
            eventPublisher.publishEvent(new UserLeveledUpEvent(userId, newLevel));
        }
    }
}
