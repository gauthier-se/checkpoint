package com.checkpoint.api.services.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.social.FollowResponseDto;
import com.checkpoint.api.dto.social.FollowUserDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.events.NotificationEvent;
import com.checkpoint.api.events.UserFollowedEvent;
import com.checkpoint.api.events.UserGainedFollowerEvent;
import com.checkpoint.api.exceptions.SelfFollowException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.dto.onboarding.OnboardingSteps;
import com.checkpoint.api.mapper.FollowMapper;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.FollowService;
import com.checkpoint.api.services.OnboardingService;

/**
 * Implementation of {@link FollowService}.
 * Manages the social graph follow/unfollow operations.
 */
@Service
@Transactional
public class FollowServiceImpl implements FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);

    private final UserRepository userRepository;
    private final FollowMapper followMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final OnboardingService onboardingService;

    public FollowServiceImpl(UserRepository userRepository, FollowMapper followMapper,
                             ApplicationEventPublisher eventPublisher,
                             OnboardingService onboardingService) {
        this.userRepository = userRepository;
        this.followMapper = followMapper;
        this.eventPublisher = eventPublisher;
        this.onboardingService = onboardingService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FollowResponseDto toggleFollow(String userEmail, UUID targetUserId) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        if (currentUser.getId().equals(targetUserId)) {
            throw new SelfFollowException();
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        boolean alreadyFollowing = userRepository.isFollowing(currentUser.getId(), targetUserId);

        if (alreadyFollowing) {
            currentUser.unfollow(targetUser);
            log.info("User {} unfollowed user {}", currentUser.getPseudo(), targetUser.getPseudo());
            return new FollowResponseDto(false, "Successfully unfollowed " + targetUser.getPseudo());
        } else {
            currentUser.follow(targetUser);
            log.info("User {} followed user {}", currentUser.getPseudo(), targetUser.getPseudo());

            eventPublisher.publishEvent(new UserFollowedEvent(currentUser.getId(), targetUser.getId()));
            eventPublisher.publishEvent(new UserGainedFollowerEvent(targetUser.getId(), currentUser.getId()));

            String message = currentUser.getPseudo() + " started following you";
            eventPublisher.publishEvent(new NotificationEvent(
                    targetUser.getId(), currentUser.getId(),
                    NotificationType.FOLLOW, targetUser.getId(), message));

            onboardingService.markStepDone(userEmail, OnboardingSteps.FOLLOW);

            return new FollowResponseDto(true, "Successfully followed " + targetUser.getPseudo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FollowUserDto> getFollowers(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        Page<User> followersPage = userRepository.findFollowersByUserId(userId, pageable);
        return followersPage.map(followMapper::toFollowUserDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FollowUserDto> getFollowing(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        Page<User> followingPage = userRepository.findFollowingByUserId(userId, pageable);
        return followingPage.map(followMapper::toFollowUserDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFollower(String userEmail, UUID followerId) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new UserNotFoundException(followerId));

        if (userRepository.isFollowing(followerId, currentUser.getId())) {
            follower.unfollow(currentUser);
            log.info("User {} removed follower {}", currentUser.getPseudo(), follower.getPseudo());
        }
    }
}
