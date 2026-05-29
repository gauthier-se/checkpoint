package com.checkpoint.api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.checkpoint.api.dto.catalog.ReviewResponseDto;
import com.checkpoint.api.dto.collection.BacklogResponseDto;
import com.checkpoint.api.dto.collection.LikedGameResponseDto;
import com.checkpoint.api.dto.collection.UserGameResponseDto;
import com.checkpoint.api.dto.collection.WishResponseDto;
import com.checkpoint.api.enums.PlayStatus;
import com.checkpoint.api.dto.list.GameListCardDto;
import com.checkpoint.api.dto.playlog.GamePlayLogResponseDto;
import com.checkpoint.api.dto.profile.ProfileUpdatedDto;
import com.checkpoint.api.dto.profile.UpdateProfileDto;
import com.checkpoint.api.dto.profile.UserProfileDto;

/**
 * Service for retrieving public user profile data.
 */
public interface ProfileService {

    /**
     * Retrieves a user's public profile by username.
     * The profile header (avatar, bio, level, badges) is always visible.
     * The {@code isFollowing} and {@code isOwner} flags are computed from the viewer's identity.
     *
     * @param username    the profile owner's username (pseudo)
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @return the user profile DTO
     */
    UserProfileDto getUserProfile(String username, String viewerEmail);

    /**
     * Retrieves a paginated list of reviews written by the given user.
     * Throws {@link com.checkpoint.api.exceptions.ProfilePrivateException} if the profile
     * is private and the viewer is not the owner.
     *
     * @param username    the profile owner's username (pseudo)
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @param pageable    pagination parameters
     * @return a page of review DTOs
     */
    Page<ReviewResponseDto> getUserReviews(String username, String viewerEmail, Pageable pageable);

    /**
     * Retrieves a paginated list of wishlist items for the given user.
     * Throws {@link com.checkpoint.api.exceptions.ProfilePrivateException} if the profile
     * is private and the viewer is not the owner.
     *
     * @param username    the profile owner's username (pseudo)
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @param pageable    pagination parameters
     * @return a page of wish DTOs
     */
    Page<WishResponseDto> getUserWishlist(String username, String viewerEmail, Pageable pageable);

    /**
     * Retrieves a paginated list of games the given user has liked.
     * Throws {@link com.checkpoint.api.exceptions.ProfilePrivateException} if the profile
     * is private and the viewer is not the owner.
     *
     * @param username    the profile owner's username (pseudo)
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @param pageable    pagination parameters
     * @return a page of liked game DTOs
     */
    Page<LikedGameResponseDto> getUserLikedGames(String username, String viewerEmail, Pageable pageable);

    /**
     * Retrieves a paginated list of games in the given user's library, optionally filtered
     * by status. Each entry includes the library owner's own {@code userRating}.
     * Throws {@link com.checkpoint.api.exceptions.ProfilePrivateException} if the profile
     * is private and the viewer is not the owner.
     *
     * @param username    the profile owner's username (pseudo)
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @param status      optional status filter (null = all statuses)
     * @param pageable    pagination parameters
     * @return a page of user-game DTOs
     */
    Page<UserGameResponseDto> getUserLibrary(String username, String viewerEmail, PlayStatus status, Pageable pageable);

    /**
     * Retrieves a paginated list of games in the given user's backlog.
     * Supports {@code sort=priority,(asc|desc)} in addition to the default date sort.
     * Throws {@link com.checkpoint.api.exceptions.ProfilePrivateException} if the profile
     * is private and the viewer is not the owner.
     *
     * @param username    the profile owner's username (pseudo)
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @param pageable    pagination parameters
     * @return a page of backlog DTOs
     */
    Page<BacklogResponseDto> getUserBacklog(String username, String viewerEmail, Pageable pageable);

    /**
     * Retrieves a paginated list of the given user's play log (journal) entries.
     * Throws {@link com.checkpoint.api.exceptions.ProfilePrivateException} if the profile
     * is private and the viewer is not the owner.
     *
     * @param username    the profile owner's username (pseudo)
     * @param viewerEmail the authenticated viewer's email, or null if anonymous
     * @param pageable    pagination parameters
     * @return a page of play log DTOs
     */
    Page<GamePlayLogResponseDto> getUserPlayLog(String username, String viewerEmail, Pageable pageable);

    /**
     * Retrieves a paginated list of public game lists for the given user.
     *
     * @param username the profile owner's username (pseudo)
     * @param pageable pagination parameters
     * @return a page of game list card DTOs
     */
    Page<GameListCardDto> getUserLists(String username, Pageable pageable);

    /**
     * Updates the authenticated user's profile information (pseudo, bio, privacy).
     *
     * @param email the authenticated user's email
     * @param dto   the profile update data
     * @return the updated profile DTO
     */
    ProfileUpdatedDto updateProfile(String email, UpdateProfileDto dto);

    /**
     * Uploads or replaces the authenticated user's profile picture.
     *
     * @param email the authenticated user's email
     * @param file  the uploaded image file
     * @return the URL of the stored picture
     */
    String updatePicture(String email, MultipartFile file);

    /**
     * Removes the authenticated user's profile picture.
     *
     * @param email the authenticated user's email
     */
    void deletePicture(String email);
}
