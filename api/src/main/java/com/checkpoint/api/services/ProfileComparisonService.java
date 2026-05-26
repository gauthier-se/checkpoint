package com.checkpoint.api.services;

import org.springframework.data.domain.Pageable;

import com.checkpoint.api.dto.profile.ProfileComparisonDto;

/**
 * Service for comparing the authenticated viewer's library and tastes with another user's.
 */
public interface ProfileComparisonService {

    /**
     * Compares the viewer's profile with the target user's profile, producing an affinity
     * score, library-size summary, and the paginated list of games they have in common.
     *
     * <p>Throws {@link IllegalArgumentException} when the viewer compares with their own
     * profile, and {@link com.checkpoint.api.exceptions.ProfilePrivateException} when the
     * target profile is private and the viewer is not following them.</p>
     *
     * @param viewerEmail    the authenticated viewer's email
     * @param targetUsername the compared user's username (pseudo)
     * @param pageable       pagination parameters for the common games list
     * @return the profile comparison DTO
     */
    ProfileComparisonDto compare(String viewerEmail, String targetUsername, Pageable pageable);
}
