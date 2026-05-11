package com.checkpoint.api.dto.export;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GDPR Article 20 export payload: every personal data category we hold about
 * the authenticated user, in a self-contained machine-readable structure.
 *
 * <p>Returned by {@code GET /api/me/export} as the body of a downloadable JSON
 * file.</p>
 */
public record UserDataExportDto(
        UserExportProfile profile,
        List<GameLibraryEntryExport> library,
        List<PlayLogExport> playLogs,
        List<ReviewExport> reviews,
        List<CommentExport> comments,
        List<WishlistEntryExport> wishlist,
        List<BacklogEntryExport> backlogs,
        List<FavoriteExport> favorites,
        List<GameListExport> gameLists,
        List<TagExport> tags,
        List<RatingExport> ratings,
        List<SocialLinkExport> socialLinks,
        LocalDateTime exportedAt) {
}
