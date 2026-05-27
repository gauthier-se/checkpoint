package com.checkpoint.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.checkpoint.api.dto.profile.ProfileUpdatedDto;
import com.checkpoint.api.dto.profile.UpdateProfileDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.PseudoAlreadyExistsException;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.mapper.LikedGameMapper;
import com.checkpoint.api.mapper.ProfileMapper;
import com.checkpoint.api.mapper.ReviewMapper;
import com.checkpoint.api.mapper.WishMapper;
import com.checkpoint.api.repositories.LikeRepository;
import com.checkpoint.api.repositories.ReviewRepository;
import com.checkpoint.api.repositories.UserGamePlayRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.repositories.WishRepository;
import com.checkpoint.api.services.impl.ProfileServiceImpl;
import com.checkpoint.api.services.OnboardingService;

/**
 * Unit tests for {@link ProfileServiceImpl} profile update methods.
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private WishRepository wishRepository;

    @Mock
    private UserGamePlayRepository userGamePlayRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private GameListService gameListService;

    @Mock
    private StorageService storageService;

    @Mock
    private ProfileMapper profileMapper;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private WishMapper wishMapper;

    @Mock
    private LikedGameMapper likedGameMapper;

    @Mock
    private OnboardingService onboardingService;

    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(
                userRepository, reviewRepository, wishRepository,
                userGamePlayRepository, likeRepository,
                gameListService, storageService,
                profileMapper, reviewMapper, wishMapper,
                likedGameMapper, onboardingService);
    }

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPseudo("oldpseudo");
        user.setEmail("alice@test.com");
        user.setBio("Old bio");
        user.setIsPrivate(false);
        user.setPicture(null);
        return user;
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update profile with new pseudo and bio")
        void updateProfile_shouldUpdateAllFields() {
            // Given
            User user = createTestUser();
            UpdateProfileDto dto = new UpdateProfileDto("newpseudo", "New bio", true);
            ProfileUpdatedDto expected = new ProfileUpdatedDto("newpseudo", "New bio", null, true);

            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
            when(userRepository.findByPseudo("newpseudo")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(profileMapper.toProfileUpdatedDto(user)).thenReturn(expected);

            // When
            ProfileUpdatedDto result = profileService.updateProfile("alice@test.com", dto);

            // Then
            assertThat(result.username()).isEqualTo("newpseudo");
            assertThat(result.bio()).isEqualTo("New bio");
            assertThat(result.isPrivate()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should skip pseudo uniqueness check when pseudo unchanged")
        void updateProfile_shouldSkipPseudoCheckWhenUnchanged() {
            // Given
            User user = createTestUser();
            UpdateProfileDto dto = new UpdateProfileDto("oldpseudo", "New bio", false);
            ProfileUpdatedDto expected = new ProfileUpdatedDto("oldpseudo", "New bio", null, false);

            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(profileMapper.toProfileUpdatedDto(user)).thenReturn(expected);

            // When
            ProfileUpdatedDto result = profileService.updateProfile("alice@test.com", dto);

            // Then
            assertThat(result.username()).isEqualTo("oldpseudo");
            verify(userRepository, never()).findByPseudo("oldpseudo");
        }

        @Test
        @DisplayName("Should throw PseudoAlreadyExistsException when pseudo taken")
        void updateProfile_shouldThrowWhenPseudoTaken() {
            // Given
            User user = createTestUser();
            User otherUser = new User();
            otherUser.setId(UUID.randomUUID());
            UpdateProfileDto dto = new UpdateProfileDto("takenpseudo", "bio", false);

            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
            when(userRepository.findByPseudo("takenpseudo")).thenReturn(Optional.of(otherUser));

            // When / Then
            assertThatThrownBy(() -> profileService.updateProfile("alice@test.com", dto))
                    .isInstanceOf(PseudoAlreadyExistsException.class);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void updateProfile_shouldThrowWhenUserNotFound() {
            // Given
            UpdateProfileDto dto = new UpdateProfileDto("pseudo", "bio", false);
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> profileService.updateProfile("unknown@test.com", dto))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updatePicture")
    class UpdatePictureTests {

        @Test
        @DisplayName("Should upload new picture and delete old one")
        void updatePicture_shouldReplaceExistingPicture() {
            // Given
            User user = createTestUser();
            user.setPicture("/uploads/profiles/old.jpg");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", "data".getBytes());

            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
            when(storageService.store(file, "profiles")).thenReturn("profiles/new-uuid.jpg");
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            String result = profileService.updatePicture("alice@test.com", file);

            // Then
            assertThat(result).isEqualTo("/uploads/profiles/new-uuid.jpg");
            verify(storageService).delete("profiles/old.jpg");
            verify(storageService).store(file, "profiles");
        }

        @Test
        @DisplayName("Should upload picture when no existing picture")
        void updatePicture_shouldUploadWhenNoPreviousPicture() {
            // Given
            User user = createTestUser();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.png", "image/png", "data".getBytes());

            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
            when(storageService.store(file, "profiles")).thenReturn("profiles/uuid.png");
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            String result = profileService.updatePicture("alice@test.com", file);

            // Then
            assertThat(result).isEqualTo("/uploads/profiles/uuid.png");
            verify(storageService, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("deletePicture")
    class DeletePictureTests {

        @Test
        @DisplayName("Should delete existing picture")
        void deletePicture_shouldDeleteAndClearField() {
            // Given
            User user = createTestUser();
            user.setPicture("/uploads/profiles/existing.jpg");

            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            profileService.deletePicture("alice@test.com");

            // Then
            verify(storageService).delete("profiles/existing.jpg");
            verify(userRepository).save(user);
            assertThat(user.getPicture()).isNull();
        }

        @Test
        @DisplayName("Should do nothing when no picture exists")
        void deletePicture_shouldDoNothingWhenNoPicture() {
            // Given
            User user = createTestUser();

            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

            // When
            profileService.deletePicture("alice@test.com");

            // Then
            verify(storageService, never()).delete(any());
            verify(userRepository, never()).save(any());
        }
    }
}
