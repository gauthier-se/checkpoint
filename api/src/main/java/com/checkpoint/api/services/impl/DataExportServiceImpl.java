package com.checkpoint.api.services.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.export.BacklogEntryExport;
import com.checkpoint.api.dto.export.CommentExport;
import com.checkpoint.api.dto.export.FavoriteExport;
import com.checkpoint.api.dto.export.GameLibraryEntryExport;
import com.checkpoint.api.dto.export.GameListEntryExport;
import com.checkpoint.api.dto.export.GameListExport;
import com.checkpoint.api.dto.export.PlayLogExport;
import com.checkpoint.api.dto.export.RatingExport;
import com.checkpoint.api.dto.export.ReviewExport;
import com.checkpoint.api.dto.export.SocialLinkExport;
import com.checkpoint.api.dto.export.TagExport;
import com.checkpoint.api.dto.export.UserDataExportDto;
import com.checkpoint.api.dto.export.UserExportProfile;
import com.checkpoint.api.dto.export.WishlistEntryExport;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.UserNotFoundException;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.DataExportService;

/**
 * Implementation of {@link DataExportService}.
 *
 * <p>The service loads the {@link User} entity once and walks its
 * cascade-managed collections to assemble the export. JPA fetches each lazy
 * collection on first access; the {@code @Transactional(readOnly = true)}
 * boundary keeps the session open for the whole assembly.</p>
 */
@Service
public class DataExportServiceImpl implements DataExportService {

    private static final Logger log = LoggerFactory.getLogger(DataExportServiceImpl.class);

    private final UserRepository userRepository;

    public DataExportServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDataExportDto exportForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        log.info("Building data export for user {} ({})", user.getPseudo(), user.getId());

        return new UserDataExportDto(
                mapProfile(user),
                mapLibrary(user),
                mapPlayLogs(user),
                mapReviews(user),
                mapComments(user),
                mapWishlist(user),
                mapBacklog(user),
                mapFavorites(user),
                mapGameLists(user),
                mapTags(user),
                mapRatings(user),
                mapSocialLinks(user),
                LocalDateTime.now());
    }

    private UserExportProfile mapProfile(User user) {
        return new UserExportProfile(
                user.getId(),
                user.getPseudo(),
                user.getEmail(),
                user.getBio(),
                user.getPicture(),
                user.getIsPrivate(),
                user.getLevel(),
                user.getXpPoint(),
                user.getCreatedAt());
    }

    private List<GameLibraryEntryExport> mapLibrary(User user) {
        return user.getUserGames().stream()
                .map(ug -> new GameLibraryEntryExport(
                        ug.getVideoGame().getId(),
                        ug.getVideoGame().getTitle(),
                        ug.getStatus(),
                        ug.getCreatedAt(),
                        ug.getUpdatedAt()))
                .sorted(Comparator.comparing(GameLibraryEntryExport::createdAt))
                .toList();
    }

    private List<PlayLogExport> mapPlayLogs(User user) {
        return user.getGamePlays().stream()
                .map(p -> new PlayLogExport(
                        p.getId(),
                        p.getVideoGame().getId(),
                        p.getVideoGame().getTitle(),
                        p.getStatus(),
                        p.getIsReplay(),
                        p.getTimePlayed(),
                        p.getStartDate(),
                        p.getEndDate(),
                        p.getOwnership(),
                        p.getScore(),
                        p.getPlatform().getName(),
                        p.getTags().stream().map(t -> t.getName()).sorted().toList(),
                        p.getCreatedAt(),
                        p.getUpdatedAt()))
                .sorted(Comparator.comparing(PlayLogExport::createdAt))
                .toList();
    }

    private List<ReviewExport> mapReviews(User user) {
        return user.getReviews().stream()
                .map(r -> new ReviewExport(
                        r.getId(),
                        r.getVideoGame().getId(),
                        r.getVideoGame().getTitle(),
                        r.getContent(),
                        r.getHaveSpoilers(),
                        r.getCreatedAt(),
                        r.getUpdatedAt()))
                .sorted(Comparator.comparing(ReviewExport::createdAt))
                .toList();
    }

    private List<CommentExport> mapComments(User user) {
        return user.getComments().stream()
                .map(c -> new CommentExport(
                        c.getId(),
                        c.getContent(),
                        c.getReview() != null ? c.getReview().getId() : null,
                        c.getGameList() != null ? c.getGameList().getId() : null,
                        c.getCreatedAt(),
                        c.getUpdatedAt()))
                .sorted(Comparator.comparing(CommentExport::createdAt))
                .toList();
    }

    private List<WishlistEntryExport> mapWishlist(User user) {
        return user.getWishes().stream()
                .map(w -> new WishlistEntryExport(
                        w.getVideoGame().getId(),
                        w.getVideoGame().getTitle(),
                        w.getPriority(),
                        w.getCreatedAt(),
                        w.getUpdatedAt()))
                .sorted(Comparator.comparing(WishlistEntryExport::createdAt))
                .toList();
    }

    private List<BacklogEntryExport> mapBacklog(User user) {
        return user.getBacklogs().stream()
                .map(b -> new BacklogEntryExport(
                        b.getVideoGame().getId(),
                        b.getVideoGame().getTitle(),
                        b.getPriority(),
                        b.getCreatedAt(),
                        b.getUpdatedAt()))
                .sorted(Comparator.comparing(BacklogEntryExport::createdAt))
                .toList();
    }

    private List<FavoriteExport> mapFavorites(User user) {
        return user.getFavorites().stream()
                .map(f -> new FavoriteExport(
                        f.getVideoGame().getId(),
                        f.getVideoGame().getTitle(),
                        f.getDisplayOrder(),
                        f.getCreatedAt()))
                .sorted(Comparator.comparing(
                        FavoriteExport::displayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<GameListExport> mapGameLists(User user) {
        return user.getGameLists().stream()
                .map(gl -> new GameListExport(
                        gl.getId(),
                        gl.getTitle(),
                        gl.getDescription(),
                        gl.getIsPrivate(),
                        gl.getEntries().stream()
                                .map(e -> new GameListEntryExport(
                                        e.getVideoGame().getId(),
                                        e.getVideoGame().getTitle(),
                                        e.getPosition()))
                                .sorted(Comparator.comparing(
                                        GameListEntryExport::position,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList(),
                        gl.getCreatedAt(),
                        gl.getUpdatedAt()))
                .sorted(Comparator.comparing(GameListExport::createdAt))
                .toList();
    }

    private List<TagExport> mapTags(User user) {
        return user.getTags().stream()
                .map(t -> new TagExport(t.getId(), t.getName(), t.getCreatedAt()))
                .sorted(Comparator.comparing(TagExport::name))
                .toList();
    }

    private List<RatingExport> mapRatings(User user) {
        return user.getRates().stream()
                .map(r -> new RatingExport(
                        r.getVideoGame().getId(),
                        r.getVideoGame().getTitle(),
                        r.getScore(),
                        r.getCreatedAt()))
                .sorted(Comparator.comparing(RatingExport::createdAt))
                .toList();
    }

    private List<SocialLinkExport> mapSocialLinks(User user) {
        return user.getSocialLinks().stream()
                .map(s -> new SocialLinkExport(s.getUrl(), s.getCreatedAt()))
                .sorted(Comparator.comparing(SocialLinkExport::createdAt))
                .toList();
    }
}
