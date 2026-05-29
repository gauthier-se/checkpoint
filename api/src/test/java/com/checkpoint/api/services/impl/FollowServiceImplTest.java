package com.checkpoint.api.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.social.FollowResponseDto;
import com.checkpoint.api.dto.social.FollowUserDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.events.NotificationEvent;
import com.checkpoint.api.exceptions.SelfFollowException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.mapper.FollowMapper;
import com.checkpoint.api.mapper.impl.FollowMapperImpl;
import com.checkpoint.api.repositories.UserRepository;

/**
 * Unit tests for {@link FollowServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.checkpoint.api.services.OnboardingService onboardingService;

    private FollowMapper followMapper;
    private FollowServiceImpl followService;

    private User currentUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        followMapper = new FollowMapperImpl();
        followService = new FollowServiceImpl(userRepository, followMapper, eventPublisher, onboardingService);

        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setEmail("user@example.com");
        currentUser.setPseudo("currentUser");

        targetUser = new User();
        targetUser.setId(UUID.randomUUID());
        targetUser.setEmail("target@example.com");
        targetUser.setPseudo("targetUser");
    }

    @Nested
    @DisplayName("toggleFollow")
    class ToggleFollow {

        @Test
        @DisplayName("should follow when not already following")
        void toggleFollow_shouldFollowWhenNotAlreadyFollowing() {
            // Given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findById(targetUser.getId()))
                    .thenReturn(Optional.of(targetUser));
            when(userRepository.isFollowing(currentUser.getId(), targetUser.getId()))
                    .thenReturn(false);

            // When
            FollowResponseDto result = followService.toggleFollow("user@example.com", targetUser.getId());

            // Then
            assertThat(result.following()).isTrue();
            assertThat(result.message()).contains("targetUser");
            assertThat(currentUser.getFollowing()).contains(targetUser);

            ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            NotificationEvent event = eventCaptor.getValue();
            assertThat(event.getRecipientId()).isEqualTo(targetUser.getId());
            assertThat(event.getSenderId()).isEqualTo(currentUser.getId());
            assertThat(event.getType()).isEqualTo(NotificationType.FOLLOW);
            assertThat(event.getMessage()).contains("currentUser");
        }

        @Test
        @DisplayName("should unfollow when already following")
        void toggleFollow_shouldUnfollowWhenAlreadyFollowing() {
            // Given
            currentUser.follow(targetUser);
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findById(targetUser.getId()))
                    .thenReturn(Optional.of(targetUser));
            when(userRepository.isFollowing(currentUser.getId(), targetUser.getId()))
                    .thenReturn(true);

            // When
            FollowResponseDto result = followService.toggleFollow("user@example.com", targetUser.getId());

            // Then
            assertThat(result.following()).isFalse();
            assertThat(result.message()).contains("targetUser");
            assertThat(currentUser.getFollowing()).doesNotContain(targetUser);
            verify(eventPublisher, never()).publishEvent(any(NotificationEvent.class));
        }

        @Test
        @DisplayName("should throw SelfFollowException when user tries to follow themselves")
        void toggleFollow_shouldThrowWhenSelfFollow() {
            // Given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(currentUser));

            // When / Then
            assertThatThrownBy(() -> followService.toggleFollow("user@example.com", currentUser.getId()))
                    .isInstanceOf(SelfFollowException.class)
                    .hasMessage("You cannot follow yourself");
        }

        @Test
        @DisplayName("should throw UserNotFoundException when target user does not exist")
        void toggleFollow_shouldThrowWhenTargetNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> followService.toggleFollow("user@example.com", unknownId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getFollowers")
    class GetFollowers {

        @Test
        @DisplayName("should return paginated followers")
        void getFollowers_shouldReturnPaginatedResults() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            User follower = new User();
            follower.setId(UUID.randomUUID());
            follower.setPseudo("follower1");
            follower.setPicture("pic.jpg");

            Page<User> userPage = new PageImpl<>(List.of(follower));
            when(userRepository.existsById(targetUser.getId())).thenReturn(true);
            when(userRepository.findFollowersByUserId(targetUser.getId(), pageable))
                    .thenReturn(userPage);

            // When
            Page<FollowUserDto> result = followService.getFollowers(targetUser.getId(), pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).pseudo()).isEqualTo("follower1");
            assertThat(result.getContent().get(0).picture()).isEqualTo("pic.jpg");
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user does not exist")
        void getFollowers_shouldThrowWhenUserNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.existsById(unknownId)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> followService.getFollowers(unknownId, pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getFollowing")
    class GetFollowing {

        @Test
        @DisplayName("should return paginated following list")
        void getFollowing_shouldReturnPaginatedResults() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            User followed = new User();
            followed.setId(UUID.randomUUID());
            followed.setPseudo("followed1");
            followed.setPicture("pic2.jpg");

            Page<User> userPage = new PageImpl<>(List.of(followed));
            when(userRepository.existsById(currentUser.getId())).thenReturn(true);
            when(userRepository.findFollowingByUserId(currentUser.getId(), pageable))
                    .thenReturn(userPage);

            // When
            Page<FollowUserDto> result = followService.getFollowing(currentUser.getId(), pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).pseudo()).isEqualTo("followed1");
            assertThat(result.getContent().get(0).picture()).isEqualTo("pic2.jpg");
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user does not exist")
        void getFollowing_shouldThrowWhenUserNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.existsById(unknownId)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> followService.getFollowing(unknownId, pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeFollower")
    class RemoveFollower {

        @Test
        @DisplayName("should remove follower when they currently follow the user")
        void removeFollower_shouldRemoveWhenFollowing() {
            // Given: targetUser follows currentUser
            targetUser.follow(currentUser);
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findById(targetUser.getId()))
                    .thenReturn(Optional.of(targetUser));
            when(userRepository.isFollowing(targetUser.getId(), currentUser.getId()))
                    .thenReturn(true);

            // When
            followService.removeFollower("user@example.com", targetUser.getId());

            // Then
            assertThat(targetUser.getFollowing()).doesNotContain(currentUser);
            assertThat(currentUser.getFollowers()).doesNotContain(targetUser);
        }

        @Test
        @DisplayName("should be a no-op when the user is not a follower")
        void removeFollower_shouldBeNoOpWhenNotFollowing() {
            // Given
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findById(targetUser.getId()))
                    .thenReturn(Optional.of(targetUser));
            when(userRepository.isFollowing(targetUser.getId(), currentUser.getId()))
                    .thenReturn(false);

            // When
            followService.removeFollower("user@example.com", targetUser.getId());

            // Then
            assertThat(currentUser.getFollowers()).doesNotContain(targetUser);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when the follower does not exist")
        void removeFollower_shouldThrowWhenFollowerNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findByEmail("user@example.com"))
                    .thenReturn(Optional.of(currentUser));
            when(userRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> followService.removeFollower("user@example.com", unknownId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
